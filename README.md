# decomposer
Microservice (de-)composition tooling

This project contains (so far):
- Parsing tools from WSDL to JSON-LD (todo)
- Dataset from Mashape.com in WSDL
- Dataset from Mashape.com in JSON-LD (todo)
- schema.org tree in json-ld (can be treated initially as JSON)

JSON-LD:
http://json-ld.org

Hydra:
http://www.hydra-cg.com

--------------
added log4j as std output
Parameters:
0: input folder path -- e.g., ./input/ %Puede volar y estar siempre en folder de proyecto
1: context file path -- e.g., ./schemaOrgTree.jsonld/ 
2: results path -- e.g., ./results/ %Puede volar y estar siempre en folder de proyecto
3: path to discoDB -- e.g., /disco/db
4: similarity threshold (reccomended for DISCO = 1.5)

