rm -f database-diagram.pdf
java -classpath ../schemacrawler-8.0.jar:../hsqldb.jar schemacrawler.Main -c hsqldb -infolevel=maximum -command=diagram -outputformat=pdf -outputfile=database-diagram.pdf
echo Database diagram is in database-diagram.pdf