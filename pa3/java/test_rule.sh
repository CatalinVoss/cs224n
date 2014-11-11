ant clean
ant
java -Xmx10g -cp "extlib/*:classes" cs224n.assignments.CoreferenceTester -path /afs/ir/class/cs224n/data/pa3/ -model rule -data dev -mistakes 20 -documents 100

