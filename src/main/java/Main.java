import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import tools.JsonUtils;
import tools.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class Main {
    public static final Logger logger =  LogManager.getLogger("Decomposer");
    public static void main(String[] args) throws IOException {

        //Cableado, Tirar los swagger de input en una carpeta que se llame ./jsons
        File inputPath = new File("./jsons/");
        // path al schema --> "./schemaOrgTree.jsonld"  "2"
        File contextFile = new File(args[0]);
        String resultsPath = "./results/";
        //path a la db disco --> "/XXX/discoDb"
        String discoDbPath = args[1];
        //threshold para considerar que una similitud es suficientemente fuerte --> e.g. "2"
        Float threshold = Float.parseFloat(args[2]);

        ObjectMapper objectMapper = new ObjectMapper();
        //Levantar el arbol schema.org a memoria
        JsonNode rootContextNode = objectMapper.readTree(contextFile);
        long timestamp = System.currentTimeMillis();

        File[] listInput = inputPath.listFiles();
        // Inicializo diccionario de WordNet para separar en terminos y stemming,
        // y la bd de disco para semantic similarity
        Utils.initializeDictionaries(discoDbPath);

        for (int i = 0; i < listInput.length ; i++) {
            FileWriter oStream = new FileWriter(resultsPath + timestamp + "_" + listInput[i].getName());
            BufferedWriter out = new BufferedWriter(oStream);

            // levanto el swagger con el parser loco
            Swagger swagger = new SwaggerParser().read(listInput[i].toString());

            try {
                System.out.println(listInput[i].getName());
                //compara el swagger actual con el arbol de schema.org
                JsonUtils.AnalyzeSwagger(swagger, rootContextNode, out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Build result construye el resumen de los resultados en un solo archivo
        BuildResult(resultsPath,timestamp, rootContextNode, threshold);

        float seconds = (System.currentTimeMillis()-timestamp)/1000;
        logger.info("The Process has taken: " + seconds + " seconds");

    }

    private static void BuildResult(String resultPath, long timestamp, JsonNode rootContextNode, Float threshold) {
        File resFolder = new File(resultPath);
        File[] resInput = resFolder.listFiles();

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(resultPath+"/"+timestamp+"_result.txt"));
            for (int i = 0; i < resInput.length; i++) {
                File resActual = resInput[i];
                if (resActual.getName().contains(String.valueOf(timestamp))){
                    BufferedReader br;
                    try {
                        br = new BufferedReader(new FileReader(resActual));
                        String line;

                        Hashtable<String,Integer> hash =new Hashtable<>();

                        while ((line = br.readLine())!= null){
                            String[] lineaActual = line.split(";");

                            //agrego este similarity al resultado final
                            // solo si es significativo (menor que el threshold)
                            if (Float.parseFloat(lineaActual[2]) <= threshold){
                                int level = 0;

                                //busco el nodo en el schema
                                JsonNode contextNode = JsonUtils.LocateInJsonTree(rootContextNode, lineaActual[1], "children", level);
                                if (!(contextNode.isMissingNode())){
                                    logger.info(contextNode.get("name"));

                                    //busco el padre del nodo en el schema, que es mas general
                                    // y por lo tanto me permite agrupar conceptos similares
                                    JsonNode parentNode = JsonUtils.LocateInJsonTree(rootContextNode, contextNode.get("name").asText(),"children",level);

                                    //si ya habia una operacion que referia al mismo concepto, sumo una mas
                                    if (hash.containsKey(parentNode.get("name").asText())){
                                        Integer newVal = hash.get(parentNode.get("name").asText()) + 1;
                                        hash.remove(parentNode.get("name").asText());
                                        hash.put(parentNode.get("name").asText(),newVal);
                                    }

                                    //sino creo una nueva linea para el concepto
                                    else hash.put(parentNode.get("name").asText(),1);
                                    logger.info(parentNode.get("name"));
                                    logger.info(hash.toString());
                                }
                            }
                        }

                        final Set<Map.Entry<String, Integer>> entries = hash.entrySet();
                        Iterator iter = entries.iterator();
                        while (iter.hasNext()){
                            Map.Entry<String,Integer> element = (Map.Entry<String, Integer>) iter.next();
                            String aux = resActual.getName().split("_")[1];
                            bw.write(aux.substring(0,aux.lastIndexOf("."))+";");
                            bw.write(element.getKey()+";"+element.getValue());
                            bw.write("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
