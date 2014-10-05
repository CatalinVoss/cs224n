#!/usr/bin/env python 
import os

dir_name = "pa1_results/"

languages = ["french", "hindi", "chinese"]
models = ["PMIModel", "IBMModel1", "IBMModel2"]
for lang in languages:
	pathname = dir_name + lang + "/"
	for model in models:
		qsubscript = '''#!/bin/bash

#$ -N %s_%s
#$ -o %s%s.out
#$ -e %s%s.error
#$ -cwd
#$ -S /bin/bash

	time java -cp ~/cs224n/pa1/java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -language %s \
  -model cs224n.wordaligner.%s -evalSet test \
  -trainSentences 10000


	''' % (lang, model, pathname, model, pathname, model, lang, model)


		qsubfile = open ("%srun_%s.submit" % (pathname, model,) , 'w')
		qsubfile.write (qsubscript)
		qsubfile.close()

		os.system ('qsub %srun_%s.submit' % (pathname, model, ))