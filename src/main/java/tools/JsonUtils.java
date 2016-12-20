package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import de.linguatools.disco.DISCO;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
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
            while (childrenElements.hasNext()) {
                JsonNode childrenElement = childrenElements.next();
                ret = CompareWithJsonTree(vectorTerms, childrenElement, childrenKey, bestDepth, bestResult, level);
                bestResult= ret[0];
                bestDepth = Float.parseFloat(ret[1]);
            }
            level--;
        }

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
        //
        Vector<String> contextTerms = Utils.separarTerminosAuxFine(sanitizedContextName);
        float totalDepthNode = 999;
        Iterator<String> iterContext = contextTerms.iterator();
        double[][] hungarianMatrix = new double[vectorTerms.size()][contextTerms.size()];
        int j = 0;
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
                        //DISCO
                        float discoValue = 1;
                        if (wordI!=null) {
                            try {
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
        //DISCO
        totalDepthNode = (float) (totalDepthNode + (0.1 * Math.abs(vectorTerms.size() - contextTerms.size())));
        if (totalDepthNode<bestDepth){
            bestResult = sanitizedContextName;
            bestDepth = totalDepthNode;
        }
        ret[0] = bestResult;
        ret[1] = valueOf(bestDepth);
        return ret;
    }

    public static void AnalyzeJson(JsonNode inputNode, JsonNode rootContextNode, String childrenName, BufferedWriter out) throws IOException {

        JsonNode auxInput = inputNode.path("name");
        if (!auxInput.isMissingNode()){
            String sanitizedInput = inputNode.get("name").toString().replace("\"","").trim();
            Vector<String> vectorTerms = Utils.separarTerminosAuxFine(sanitizedInput);
            if (!vectorTerms.isEmpty()){
                String[] mejorResultActual = CompareWithJsonTree(vectorTerms, rootContextNode, "children", 999, "", 0);
                try {
                    out.write(String.valueOf(vectorTerms+";"));
                    logger.info(String.valueOf(vectorTerms+";"));
                    for (int i = 0; i < mejorResultActual.length ; i++) {
                        out.write(mejorResultActual[i]+";");
                        logger.info(mejorResultActual[i]+";");
                    }
                    out.write("\n");
                } catch (IOException e) {
                    logger.error(e);
                }
                logger.info("\n");
            }
        }
        JsonNode children = inputNode.path(childrenName);
        if (!children.isMissingNode()){
            Iterator<JsonNode> childrenElements = children.elements();
            while (childrenElements.hasNext()){
                JsonNode childrenElement = childrenElements.next();
                AnalyzeJson(childrenElement, rootContextNode, childrenName, out);
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
