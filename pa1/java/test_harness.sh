# java -cp ~/cs224n/pa1/java/classes cs224n.assignments.WordAlignmentTester -dataPath /afs/ir/class/cs224n/data/pa1 -model cs224n.wordaligner.BaselineWordAligner -evalSet miniTest -verbose

java -cp ~/cs224n/pa1/java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.IBMModel2 -evalSet dev \
  -trainSentences 5000
