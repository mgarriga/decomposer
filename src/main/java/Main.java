import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {


        // Open a valid json(-ld) input file
        InputStream inputStream = null;
        try {
            inputStream   = new FileInputStream("./sample.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
        // Number or null depending on the root object in the file).
        Object jsonObject = null;
        try {
            jsonObject = JsonUtils.fromInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create a context JSON map containing prefixes and definitions
        Map context=new HashMap();


        // Customise context...

        // Create an instance of JsonLdOptions with the standard JSON-LD options
            JsonLdOptions options = new JsonLdOptions();

        // Customise options...
        // Call whichever JSONLD function you want! (e.g. compact)
            Map<String,Object> compact;

            try {

                compact = JsonLdProcessor.compact(jsonObject, context, options);



                try {
                    // Print out the result (or don't, it's your call!)
                   System.out.println(JsonUtils.toPrettyString(compact));


                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (JsonLdError jsonLdError) {
                jsonLdError.printStackTrace();
            }

    }
}
