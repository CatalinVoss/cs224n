#!/usr/bin/env python
# 
# Driver to generate data for plots with various seed sizes and nearest neighbors
# 

import os
import numpy as np

root = "10m_norm_"
NUM_TRAIN = 8544 + 1
NUM_DEV = 1101 + 1
NUM_TEST = 2210 + 1
pathname = root + "bigramBatch/"

trainRanges = [i for i in range(1, NUM_TRAIN, 25)]
trainRanges.append(NUM_TRAIN)

devRanges = [i for i in range(1, NUM_DEV, 20)]
devRanges.append(NUM_DEV)
testRanges = [i for i in range(1, NUM_TEST, 20)]
testRanges.append(NUM_TEST)


for idx in range(len(trainRanges) - 1):
	start_idx = trainRanges[idx]
	end_idx = trainRanges[idx + 1]

	name = "train_" + str(start_idx) + "_" + str(end_idx)
	qsubscript = '''#!/bin/bash

#$ -N %s
#$ -o %s%s.output
#$ -e %s%s.error
#$ -cwd
#$ -S /bin/bash

	./bigram_construct_matrices.py --batch true --start_idx %d --end_idx %d  --sentence_file trainSentences.txt


	''' % (name, pathname, name, pathname, name, start_idx, end_idx);


	qsubfile = open ("%srun_%s.submit" % (pathname, name, ) , 'w')
	qsubfile.write (qsubscript)
	qsubfile.close()

	os.system ('qsub %srun_%s.submit' % (pathname, name, ))

for idx in range(len(devRanges) - 1):
	start_idx = devRanges[idx]
	end_idx = devRanges[idx + 1]

	name = "dev_" + str(start_idx) + "_" + str(end_idx)
	qsubscript = '''#!/bin/bash

#$ -N %s
#$ -o %s%s.output
#$ -e %s%s.error
#$ -cwd
#$ -S /bin/bash

	./bigram_construct_matrices.py --batch true --start_idx %d --end_idx %d  --sentence_file devSentences.txt


	''' % (name, pathname, name, pathname, name, start_idx, end_idx);


	qsubfile = open ("%srun_%s.submit" % (pathname, name, ) , 'w')
	qsubfile.write (qsubscript)
	qsubfile.close()

	os.system ('qsub %srun_%s.submit' % (pathname, name, ))

# for idx in range(len(testRanges) - 1):
# 	start_idx = testRanges[idx]
# 	end_idx = testRanges[idx + 1]

# 	name = "test_" + str(start_idx) + "_" + str(end_idx)
# 	qsubscript = '''#!/bin/bash

# #$ -N %s
# #$ -o %s%s.output
# #$ -e %s%s.error
# #$ -cwd
# #$ -S /bin/bash

# 	./bigram_construct_matrices.py --batch true --start_idx %d --end_idx %d  --sentence_file testSentences.txt


# 	''' % (name, pathname, name, pathname, name, start_idx, end_idx);


# 	qsubfile = open ("%srun_%s.submit" % (pathname, name, ) , 'w')
# 	qsubfile.write (qsubscript)
# 	qsubfile.close()

# 	os.system ('qsub %srun_%s.submit' % (pathname, name, ))