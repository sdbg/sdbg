mkdir build\classes
java -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT -Djava.net.useSystemProxies=true -cp build/classes;build/nobuto.jar de.exware.nobuto.Main $1 $2 $3 $4
