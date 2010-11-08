JAVA="java -XX:ThreadStackSize=256k -XX:+UseCompressedOops -XX:+UseParallelGC"
CP="lib/gnat.jar"
DICT="dictionaries"

#GO terms
nohup ${JAVA} -cp ${CP} -Xmx2000M gnat.server.dictionary.DictionaryServer 56099 ${DICT}/goMesh/ &

