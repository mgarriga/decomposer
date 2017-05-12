package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import de.linguatools.disco.DISCO;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static java.lang.String.valueOf;

/**
 * Created by Tincho on 12/12/2016.
 */
public class JsonUtils {
    public static final Logger logger =  LogManager.getLogger("JsonUtils");

    private static String[] CompareWithJsonTree(Vector<String> vectorTerms, JsonNode contextNode, String childrenKey, float bestDepth, String bestResult, int level) {
        JsonNode children = contextNode.path(childrenKey);
        String [] ret = new String[2];
        if (!children.isMissingNode()){
            Iterator<JsonNode> childrenElements = children.elements();
            level++;
            //La comparación es depth first (primero los conceptos mas especificos del schema)
            // asique recorro y llamo recursivamente con los hijos
            while (childrenElements.hasNext()) {
                JsonNode childrenElement = childrenElements.next();
                ret = CompareWithJsonTree(vectorTerms, childrenElement, childrenKey, bestDepth, bestResult, level);
                bestResult= ret[0];
                bestDepth = Float.parseFloat(ret[1]);
            }
            level--;
        }

        //Preparo el vector de terminos del schema
        String sanitizedContextName = contextNode.get("name").toString().replace("\"","").trim();
        // For debuggin purposes
        /*if (sanitizedContextName.equalsIgnoreCase("WearAction")){  //insert context term here
            Iterator<String> it = vectorTerms.iterator();
            while (it.hasNext()){
                if (it.next().equalsIgnoreCase("tracking")){   // insert input term here
                    System.out.println("here!");
                }
            }
        }*/

        Vector<String> contextTerms = Utils.separarTerminosAuxFine(sanitizedContextName);
        //System.out.println(contextTerms);
        float totalDepthNode = 999;
        Iterator<String> iterContext = contextTerms.iterator();
        double[][] hungarianMatrix = new double[vectorTerms.size()][contextTerms.size()];
        int j = 0;

        //Aca viene la magia, comparo termino contra termino del input y del context
        // usando DISCO para semantic similarity y hungarian para quedarme con la mejor
        while (iterContext.hasNext()){
            int bestAux = 0;
            String contextTermActual = iterContext.next().trim();
            try {
                IndexWord wordC = Utils.dictionary.lookupIndexWord(POS.NOUN, contextTermActual);
                if (wordC!=null) {
                    Iterator<String> iterInput = vectorTerms.iterator();
                    int i=0;
                    while (iterInput.hasNext()){
                        String inputTermActual = iterInput.next().trim();
                        IndexWord wordI = Utils.dictionary.lookupIndexWord(POS.NOUN, inputTermActual);

                        float discoValue = 1;
                        if (wordI!=null) {
                            try {
                                // llamo efectivamente a disco con el lemma de cada termino
                                discoValue = Utils.disco.semanticSimilarity(wordI.getLemma(),wordC.getLemma(), DISCO.SimilarityMeasure.KOLB);

                                if (discoValue > 0) {
                                    if (discoValue >= 1) discoValue = 0;
                                    else discoValue = 1 - discoValue;
                                }
                                else discoValue = 1;
                            } catch (IOException e) {
                                logger.error("Problem with DISCO similarity: ", e );
                            }

                        } else {
                            discoValue = 1;
                        }

                        hungarianMatrix[i][j]=discoValue;
                        bestAux += discoValue;

                        i++;
                    }
                } else {
                    for (int i = 0; i < vectorTerms.size(); i++) {
                        hungarianMatrix[i][j] = 1;
                    }
                    //DISCO
                    bestAux += 1;
                }
                if (totalDepthNode == 999) totalDepthNode = bestAux;
                else totalDepthNode += bestAux;
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
            j++;
        }
        //calculo el valor de similitud total para los terminos del input
        // y del nodo context (schema) actual usando hungarian
        HungarianAlgorithm hg = new HungarianAlgorithm(hungarianMatrix);
        int[] resultMatrix =  hg.execute();
        totalDepthNode = 0;
        for (int i = 0; i < resultMatrix.length; i++) {
            if (resultMatrix[i] != -1){
                totalDepthNode+=hungarianMatrix[i][resultMatrix[i]];
            } else{
                //DISCO
                totalDepthNode+=1;
            }
        }
        //penalizar con 0.1 por cada termino no usado
        totalDepthNode = (float) (totalDepthNode + (0.1 * Math.abs(vectorTerms.size() - contextTerms.size())));
        if (totalDepthNode<bestDepth){
            bestResult = sanitizedContextName;
            bestDepth = totalDepthNode;
        }
        ret[0] = bestResult;
        ret[1] = valueOf(bestDepth);
        return ret;
    }

    public static void AnalyzeSwagger(Swagger inputNode, JsonNode rootContextNode, BufferedWriter out) throws IOException {

        // Paths contiene los "recursos" en swagger
        Map<String,Path> pathsMap = inputNode.getPaths();

        for(Map.Entry<String,Path> entry: pathsMap.entrySet()) {

            //preparo el path actual sacando caracteres extranios y separando en terminos
            String sanitizedInput = entry.getKey();
            sanitizedInput = sanitizedInput.replaceAll("[^a-zA-Z]", "-");
            sanitizedInput = sanitizedInput.substring(1);
            if (sanitizedInput.startsWith("-")) sanitizedInput = sanitizedInput.replaceFirst("-","");
            Vector<String> vectorTerms = Utils.separarTerminosAuxFine(sanitizedInput);
            vectorTerms = Utils.removeDuplicates(vectorTerms);
            System.out.println(vectorTerms);


            if (!vectorTerms.isEmpty()) {

                //comparo el vector de terminos del path actual contra el schema
                String[] mejorResultActual = CompareWithJsonTree(vectorTerms, rootContextNode, "children", 999, "", 0);
                try {
                    out.write(String.valueOf(vectorTerms + ";"));
                    logger.info(String.valueOf(vectorTerms + ";"));
                    for (int i = 0; i < mejorResultActual.length; i++) {
                        out.write(mejorResultActual[i] + ";");
                        logger.info(mejorResultActual[i] + ";");
                    }
                    out.write("\n");
                } catch (IOException e) {
                    logger.error(e);
                }
                logger.info("\n");
            }
        }

    }


    public static JsonNode LocateInJsonTree(JsonNode node, String nodeName, String childrenName, int level){

        String nameActual = node.get("name").toString().replace("\"","");
        if (nameActual.equalsIgnoreCase(nodeName)&&level<2){
            return node;
        }
        JsonNode children = node.path(childrenName);
        if (!children.isMissingNode()){
            level++;
            Iterator<JsonNode> childrenElements = children.elements();
            while (childrenElements.hasNext()){
                JsonNode childrenElement = childrenElements.next();
                if (nodeName.equalsIgnoreCase(childrenElement.get("name").toString().replace("\"",""))){
                    if (level>=2){
                        return node;
                    } else {
                        return childrenElement;
                    }
                }
                JsonNode res = LocateInJsonTree(childrenElement, nodeName, childrenName,level);
                if (!(res.isMissingNode())) return res;
            }
        }
        else return (MissingNode.getInstance());
        return MissingNode.getInstance();
    }
}
