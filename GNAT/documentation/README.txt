-- GNAT -- Gene mention Normalization Across Taxa --
----------------------------------------------------

[Note for the notoriously impatient: If you just want to run GNAT, see the 
"Main Classes" section further down follow the instructions in INSTALLATION.txt]


GNAT is a package to recognize and identify gene names in biomedical text.

GNAT performs a series of steps, first to recognize names of genes, species,
and other terms in each text it is given; assign candidate IDs (of all possible
genes that share the found name; and then to disambiguate between the 
candidates to find a single identifier (or as few as possible) pointing to an
EntrezGene record for that gene.

GNAT comes in two parts, a client and a server, although both can run on the 
same machine.

Server
------
A server is able to handle
- requests for named entity recognition: by submitting plain texts, PubMed IDs,
  and/or PMC IDs; candidate IDs will be returned with positional and other 
  information; 
  - a server can cache results if PubMed IDs / PMC IDs are used
- requests to obtain information on individual genes; an SQL database is
  needed at the server side (at least on a machine visible to the server);
but not each server has to provide each of these steps, thus a mix of servers
can be invoked by the client to address them separately.

There are two options for server-side NER requests: services and dictionaries 
(see packages gnat.server.* and gnat.server.dictionary.*). Services are 
basically wrappers around dictonaries, and they can download texts, pre-process
texts, cache results, before sending them to dictionaries. Dictionaries handle
the actual named entity recognition requests. As dictionaries run on individual
ports, and many dictionaries might be needed, a service can thus 'hide' all 
these functions behind a single port that is visible from the outside. An 
examplary wrapper service is gnat.server.GnatService, which takes texts, PubMed
IDs, and PMC IDs, and can perform gene NER, GO term NER, species NER, and
gene normalization (although this last step should be --in a developmental 
environment-- typically handled by the client).

For more information on dictionaries, see dictionaries/readme.dictionaries.txt.

To obtain information on individual genes that are required for disambiguation 
(normalization, identification, grounding) of genes, the client typically needs
access to a local (or visible) database. However, these can be provided through
a gnat.server.GeneService, again essentially a wrapper that hides the actual 
database from direct, outside access and also makes access by the client more 
transparent.

Client
------
A client is responsible for
- loading and pre-processing texts (or obtain IDs if an invoked server can 
  handle the download/pre-processing/caching);
- invoke a series of filtering steps to recognize and disambiguate gene
  names, and potentially other entities such as species names, GeneOntology
  terms, MeSH terms, etc; some of these steps can be provided by servers,
  which have to called at the proper time in a processing pipeline by the 
  client;
- load a repository of genes that contains information on individual genes,
  needed for disambiguation (as aforementioned, this can be obtained using
  a GeneService running remotely);
- store the final results.

The filtering steps in a client's processing pipeline are thought to
- annotate entities, such as genes and species;
- assign identifiers to these entities, or lists of candidates;
- remove false positive annotations;
- rank candidate IDs;
- disambiguate gene candidate IDs;
- contact servers to perform any of these steps if available.
Filters can be found in the gnat.client.filter.* package; for instance, some
filters concerned with NER in gnat.client.filter.ner.*, or others for step-wise
identification in gnat.client.filter.nei.*.






Requirements
------------

Client and Server:
- GNAT Jars and configuration files
- Third-party libraries, see libraries.readme.txt

Server:
- dictionaries, each will take 100MB to 2GB of memory (1-2GB for species with 
  many known genes and aliases, such as mouse, human, rat, fruit fly)
- RDBMS, for instance, MySQL, to save gene-specific information such as 
  species, GO terms, tissue specificity, protein length, interactions, ...




Main classes
------------

To run GNAT, check the Pipeline classes in gnat.client.*, for example, the 
DefaultPipeline.

Several such classes are provided for users not interested in developing their
own pipelines, but would rather ran GNAT as-is, using predefined filtering
steps and output formats. Probably the most useful supported in/output formats
are plain text to tabular, and XML to XML:
- JustAnnotate: takes a directory of *.txt files as input, runs GNAT on each 
  such text file, and prints a list of recognized genes and IDs in a tabular 
  format. To see examples, take a look at the files in the folders texts/test/ 
  and texts/test100/. Conventions: if your files originate from PubMed, name
  them accordingly as <pubmedid>.txt.
- JustAnnotateInline: takes a directory of *.xml files as input, runs GNAT on
  each, and annotates each text.
  Supported input formats are 
  - plain text in .txt files (output will be enclosed by <document> tags)
  - MedlineCitation and PubmedArticle, as single documents or as collections
    inside MedlineCitationSet and PubmedArticleSet, respectively.
  GNAT guesses the file type from the file names/extensions, therefore it is
  best to follow these conventions:
  - *.medline.xml files contain single documents
  - *.medlines.xml files contain Sets
  - NCBI Medline FTP download file names (medline[year]n[number].xml
    can contain either MedlineCitation or PubmedArticle Sets; those files can
    optionally be GZipped (.gz); the output will in any case be unzipped.
  The current DTD for PubmedArticle(Set)s can be found here:
    http://www.ncbi.nlm.nih.gov/corehtml/query/DTD/pubmed_120101.dtd
  and MedlineCitation(Sets) here:
    http://www.nlm.nih.gov/databases/dtd/nlmmedlinecitationset_120101.dtd
- Example files can be found in texts/test and texts/test_xml/.
- To call JustAnnotate or JustAnnotateInline, see the shell scripts in the
  scripts/ folder.



Configuration files
-------------------

- isgn_properties.xml - main configuartion file for client and server
- server_properties.xml - additional configuration settings for servers
- config/
  - taxonToServerPort.txt - mapping of NCBI taxon IDs to server addresses and
    port that serve gene NER for each respective species
  - runAdditionalFilters.txt - loaded by gnat.filter.RunAdditionalFilters
    to set up additional gnat.filter.Filter steps at runtime
  - geneNerDictionarServers.txt 



Publications:
[1] Jörg Hakenberg, Martin Gerner, Maximilian Haeussler, Illés Solt, Conrad
    Plake, Michael Schroeder, Graciela Gonzalez, Goran Nenadic, and Casey M. 
    Bergman: "The GNAT library for local and remote gene mention normalization"
    Bioinformatics, 2011, Advance access 3 August 2011
    DOI: 10.1093/bioinformatics/btr455

[2] Jörg Hakenberg, Conrad Plake, Loic Royer, Hendrik Strobelt, Ulf Leser, 
    Michael Schroeder: "Gene mention normalization and interaction extraction
    with context models and sentence motifs". Genome Biology 2008, 9:S14.
    Abstract: http://genomebiology.com/2008/9/S2/S14/abstract
    PubMed:   http://www.ncbi.nlm.nih.gov/sites/entrez/18834492

[3] Jörg Hakenberg, Conrad Plake, Robert Leaman, Michael Schroeder, and 
    Graciela Gonzalez: "Inter-species normalization of gene mentions with
    GNAT".
    Bioinformatics (2008) 24 (16):i126-i132.
    DOI:    10.1093/bioinformatics/btn299
    PubMed: http://www.ncbi.nlm.nih.gov/pubmed/18689813

More on gene mention normalization:
[4] Florian Leitner et al.: "Overview of BioCreative II gene normalization".
    Genome Biology 2008, 9:S3.
    Abstract: http://genomebiology.com/2008/9/S2/S3/abstract
    PubMed: http://www.ncbi.nlm.nih.gov/sites/entrez/18834494

[5] Genome Biology's Special Issue on BioCreative II:
    http://genomebiology.com/supplements/9/S2

[6] Minlie Huang, Jingchen Liu, and Xiaoyan Zhu: "GeneTUKit: a software for 
    document-level gene normalization".
	Bioinformatics, 2011, 27(7):1032-1033
	DOI: 10.1093/bioinformatics/btr042 
