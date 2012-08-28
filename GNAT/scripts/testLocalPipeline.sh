# Checks whether a simple, local GNAT processing pipeline is up and running 
# properly.
# A "local" pipeline requires
# - a local database with gene information; the DB server can run on
#   any machine that is visible from the machine where you run the client;
#   specify DB URL and user/password in isgnproperties.xml
# - memory-resident dictionaries that run on the same machine as the client
#   (this script)
# For more information on how to set up this test, see INSTALLATION.txt in
# the documentation/ folder.

java -cp lib/gnat.jar:lib/mysql-connector.jar gnat.tests.LocalPipelineTest
