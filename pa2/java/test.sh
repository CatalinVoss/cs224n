# Rebuild
ant clean
ant

# Run
java -Xmx1g -cp classes cs224n.assignment.PCFGParserTester -parser cs224n.assignment.PCFGParser -data treebank
