# Decomposer
## Microservice de-composition tooling
### Introduction
This tool compares specification artifacts from a monolithic application with regard to a given reference model, with the goal of providing insigths about possible decompositions of the monolith in cohesive, well-bounded and fine-grained microservices.

At this time, this ongoing work supports input artifacts in the form of JSON specifications, but we intend to provide built-in support for java interfaces/WSDL among others. 

Besides, the reference model provided with (and supported by) the tool is the schema.org shared vocabulary, specified in JSON-LD format (schemaOrgTree.jsonld). However, any other reference model could be used as well, if it is tree-shaped and described in a json (or json document.

The decomposition process consists of a semantic comparison of concepts in input specifications w.r.t. concepts in the reference model, to then devise the candidate microservices upon the commonalities found. The semantics is supported by the DISCO semantic similarity library (linguatools.de/disco/disco_en.html). 

## Current status
The project is at an early stage of development. However below you can find the documentation to run it as a bounded support to the activities involved in the decomposition process.

## Content (so far):
- Java classes: Main (entry point); Utils (string sanitizing, dictionaries, ...); JsonUtils (json tree parsing, concept comparison); HungarianAlgorithm (supports finding optimal assignments of concepts).
- pom.xml: maven dependencies.
- disco jar library: latest version is provided since it is not available in Mvn repositories (should be manually added to the build path after cloning the repo).
- schemaOrgTree.jsonld: the schema.org shared vocabulary, initially supported as reference model.
- input.zip: a dataset of input specifications in json format, crawled from mashape.com marketplace, a repo of publicly available service APIs.
- stoplist.txt: a list of stopwords to remove meaningless words before semantic comparison.

## Setup and usage
Unfortunately, we are not free from certain manual setup steps before safe usage of the tool:
- Clone the repo.
- Add disco jar library to the build path
- Download and extract a disco word space (http://www.linguatools.de/disco/disco-download_en.html). So far we've tested the English-wikipedia-based: enwiki-20130403-sim-lemma-mwl-lc
- add log4j configuration file to the classpath (optional)

After these steps, you should be ready to start using the tool. The main method expects three parameters:
- path to the reference model context file -- e.g., './schemaOrgTree.jsonld'
- path to the disco words database folder -- e.g., '/home/discoDb' (or wherever you downloaded it)
- threshold to consider semantic similarity between concepts, which will be compared with DISCO results. This value may range from 0 (nothing is considered), to 2 (every similarity is considered, as low or subtle it may be). Recommended threshold = 1.5.

## Output
Finally, the output of the tool (in folder './results') will consist of:
- One .txt output for each input file containing all the concepts found in the input operations/resources and the corresponding matching concept in the reference model, with the associated matching score.
- A final .txt of results for all input specifications, containing the best concept matchings found for each, also considering the threshold value defined for the input.

## Follow Up
We are working in the following open issues for this project. Feel free to fork us and explore your own:
- Support for non functional aspects to weight the decomposition process -- e.g., response time, resource allocation, cost.
- Automatic generation of microservice specifications in Json-ld/Hydra, based in the suggested decompositions; and augumented with the related concepts in the reference model.Ref.: json-ld.org -- hydra-cg.com
- Include built-in support for parsing any specification artifact (e.g., java interfaces, WSDL specifications) to jsonLd, to reduce the preprocessing effort for the end user.
- Provide other built-in reference models. Currently we support schema.org and any other tree-shaped reference model described in json/json-ld. Furthermore, we look forward to include other vocabularies such as FOAF, or ontologies based in UFO. Ref.: foaf.org -- oxygen.informatik.tu-cottbus.de/drupal7/ufo/  
