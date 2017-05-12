package tools;

import de.linguatools.disco.CorruptConfigFileException;
import de.linguatools.disco.DISCO;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.dictionary.MorphologicalProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by aderenzis on 14/11/16.
 */

public class Utils {
    private int MaxWordLength = 50;
    private static MorphologicalProcessor morph;
    private static boolean IsInitialized = false;
    public static HashMap AllWords = null;
    public static final Logger logger =  LogManager.getLogger("Utils");
    public static Dictionary dictionary = null;
    static Vector<String> vStopWords = null;
    static DISCO disco = null;

    public static boolean initializeDictionaries(String discoDbPath){

        try {

            AllWords = new HashMap ();


            dictionary = Dictionary.getDefaultResourceInstance();
            morph = dictionary.getMorphologicalProcessor();
            BufferedReader stoplistFile = new BufferedReader(new FileReader("./stoplist.txt"));
            String line;
            vStopWords = new Vector();
            while ((line = stoplistFile.readLine()) != null) {
                vStopWords.add(line);
            }
            stoplistFile.close();
            try {
                disco = new DISCO(discoDbPath, false);
            } catch (FileNotFoundException | CorruptConfigFileException ex) {
                logger.error("Error creating DISCO instance: ", ex);
                return false;
            }
            IsInitialized = true;
            return true;
        } catch (Exception e) {
            logger.error("Error Initializing Dictionaries: ", e);
            return false;
        }
    }

    public static boolean isWord (String wordQuery)
    {
        IndexWord idxWord = null;
        try {
            idxWord = dictionary.getIndexWord(POS.NOUN, wordQuery);
        } catch (JWNLException e) {
            logger.error("Problem with JWNL dictionaries: ", e );
        }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.VERB, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.ADJECTIVE, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.ADVERB, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        return idxWord != null;
    }

    public static Vector separarTerminosAuxFine(String term)
    {
        Vector vec = new Vector();
        if(term.length()>0)
        {
            boolean mayus =false;
            String ret="";
            String retMayus="";
            char lastMayus=0;
            char charAux;
            if(term.charAt(0)>=65 && term.charAt(0)<=90) // Si es mayuscula la 1er letra
            {
                charAux= (char) (term.charAt(0)+32); // guarda la minuscula
                ret=Character.toString(charAux); // ret almaceno la letra
                retMayus=Character.toString(charAux); // retMayus almaceno
                mayus=true;
            }
            else
                ret=Character.toString(term.charAt(0)); // si no es mayuscula se almacena el char en ret
            for(int i=1;i< term.length();i++)
            {
                if(term.charAt(i)>=65 && term.charAt(i)<=90) // Si es una mayuscula
                {

                    if(!mayus) //Es la primer Mayuscula
                    {
                        if(retMayus.length()>1) // Ya existia anteriormente una seguidilla de mayusculas
                        {
                            if(isWord(lastMayus+ret))//es una palabra la ultima mayuscula + minusculas
                            {
                                vec.add(retMayus.substring(0, retMayus.length()-1));
                                vec.add(lastMayus+ret);
                                lastMayus=0;
                            }
                            else
                            {
                                vec.add(retMayus);
                                vec.add(ret);
                                lastMayus=0;
                            }
                        }
                        else // No existia anteriormente una seguidilla de mayusculas
                            if(ret.length()>0)
                                vec.add(ret);

                        mayus=true;
                        charAux= (char) (term.charAt(i)+32);
                        ret=Character.toString(charAux);
                        retMayus=Character.toString(charAux);
                    }
                    else //No es la primer mayuscula consecutiva
                    {
                        charAux= (char) (term.charAt(i)+32);
                        retMayus = retMayus+charAux;
                        ret="";
                    }


                }
                else //No es una Mayuscula
                {
                    if(term.charAt(i) == 45 || term.charAt(i)== 95 || esNumero(term.charAt(i))) //  Si es _ o -
                    {
                        if(ret.length()>0) // si el guion esta despues de una acumulacion de Minusculas
                        {
                            vec.add(ret);
                            ret="";
                            retMayus="";
                        }
                        else if(retMayus.length()>0) // si el guion esta despues de una acumulacion de Mayusculas
                        {
                            vec.add(retMayus);
                            retMayus="";
                        }

                        mayus=false;
                    } // No es mayuscula ni _ ni - ni Numero// es una letra minuscula
                    else
                    {
                        if(mayus) // la Letra anterior era una mayuscula
                        {
                            lastMayus= (char) (term.charAt(i-1)+32);
                            ret=ret+term.charAt(i);
                            mayus=false;
                        }
                        else // la letra anterior no era mayuscula
                        {
                            ret=ret+term.charAt(i);
                        }

                    }
                }
            }
            if(ret.length()>0 | retMayus.length()>1)
            {
                if(retMayus.length()>1) // Ya existia anteriormente una seguidilla de mayusculas
                {
                    if(lastMayus != 0 && ret.length()>0 && isWord(lastMayus+ret)) // Es un && porque si lastMayus es 0 no debe entrar al metodo isWord.
                    {
                        vec.add(retMayus.substring(0, retMayus.length()-1));
                        vec.add(lastMayus+ret);
                    }
                    else
                    {
                        if(retMayus.length()>1);
                        vec.add(retMayus);
                        if(ret.length()>0)
                            vec.add(ret);
                    }
                }
                else
                    vec.add(ret);
            }
        }
        try {
            vec = removeStopWords(vec);
        } catch (IOException e) {
            logger.error(e);
        }
        return vec;
    }

    public static Vector<String> removeStopWords(Vector v1) throws IOException
    {

        boolean b = v1.removeAll(vStopWords);
        return v1;
    }

    private static boolean esNumero(char charAt) {

        return (48<=charAt && charAt<=57);
    }

    public static Vector<String> removeDuplicates(Vector<String> vectorTerms) {
        Vector<String> newVector = new Vector<>();
        IndexWord idxActual = null;
        IndexWord idxAux = null;
       // System.out.println("antes: " + vectorTerms.toString());
        for(int i = 0; i < vectorTerms.size();i++){
            boolean remove = false;
            String wordQuery = Stem(vectorTerms.get(i));
            for (int j = i+1; j < vectorTerms.size();j++){
                if(wordQuery.equalsIgnoreCase(Stem(vectorTerms.get(j)))) {
                    remove = true;
                    break;
                }
            }
            if (!remove) {
                String elem = vectorTerms.get(i);
                newVector.add(elem);
            }
        }
       // System.out.println("despues: " + newVector.toString());
        return newVector;
    }

    public static String StemWordWithWordNet(String word)
    {
        if ( !IsInitialized )
            return word;
        if ( word == null ) return null;
        if ( morph == null ) morph = dictionary.getMorphologicalProcessor();

        IndexWord w;
        try
        {
            w = morph.lookupBaseForm( POS.VERB, word );
            if ( w != null )
                return w.getLemma().toString ();
            w = morph.lookupBaseForm( POS.NOUN, word );
            if ( w != null )
                return w.getLemma().toString();
            w = morph.lookupBaseForm( POS.ADJECTIVE, word );
            if ( w != null )
                return w.getLemma().toString();
            w = morph.lookupBaseForm( POS.ADVERB, word );
            if ( w != null )
                return w.getLemma().toString();
        }
        catch ( JWNLException e )
        {
        }
        return null;
    }

    public static String Stem(String word)
    {
        // check if we already know the word
        String stemmedword = (String) AllWords.get( word );
        if ( stemmedword != null )
            return stemmedword; // return it if we already know it

        stemmedword = StemWordWithWordNet (word);

        if ( stemmedword != null )
        {
            // word was recognized and stemmed with wordnet:
            // add it to hashmap and return the stemmed word
            AllWords.put( word, stemmedword );
            return stemmedword;
        }
        // word could not be stemmed by wordnet,
        // thus it is no correct english word
        // just add it to the list of known words so
        // we won't have to look it up again
        AllWords.put( word, word );
        return word;
    }




}