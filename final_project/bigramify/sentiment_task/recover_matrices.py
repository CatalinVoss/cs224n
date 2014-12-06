#!/usr/bin/env python
#
#
import numpy as np

#path of barley output
DATAPATH = "10m_norm_bigramBatch/"


NUM_TRAIN = 8544 + 1
NUM_DEV = 1101 + 1
NUM_TEST = 2210 + 1

trainRanges = [i for i in range(1, NUM_TRAIN, 25)]
trainRanges.append(NUM_TRAIN)

devRanges = [i for i in range(1, NUM_DEV, 20)]
devRanges.append(NUM_DEV)
testRanges = [i for i in range(1, NUM_TEST, 20)]
testRanges.append(NUM_TEST)

#make trainMatrix
first = True
for idx in range(len(trainRanges) - 1):
	start_idx = trainRanges[idx]
	end_idx = trainRanges[idx + 1]
	rows = np.load(DATAPATH + str(start_idx) + "_" + str(end_idx) + "_" + "pred_bigrams_" + 'trainSentencesMatrix.npy')
	if first:
		trainMatrix = rows
		first = False
	else:
		trainMatrix = np.concatenate((trainMatrix, rows), axis = 0)

np.save('10m_norm_bigrams_trainSentencesMatrix', trainMatrix)

#make devMatrix
first = True
for idx in range(len(devRanges) - 1):
	start_idx = devRanges[idx]
	end_idx = devRanges[idx + 1]
	rows = np.load(DATAPATH + str(start_idx) + "_" + str(end_idx) + "_" + "pred_bigrams_" + 'devSentencesMatrix.npy')
	if first:
		devMatrix = rows
		first = False
	else:
		devMatrix = np.concatenate((devMatrix, rows), axis = 0)

np.save('10m_norm_bigrams_devSentencesMatrix', devMatrix)


#make testMatrix
first = True
for idx in range(len(testRanges) - 1):
	start_idx = testRanges[idx]
	end_idx = testRanges[idx + 1]
	rows = np.load(DATAPATH + str(start_idx) + "_" + str(end_idx) + "_" + "pred_bigrams_" + 'testSentencesMatrix.npy')
	if first:
		testMatrix = rows
		first = False
	else:
		testMatrix = np.concatenate((trainMatrix, rows), axis = 0)

np.save('2m_norm_bigrams_testSentencesMatrix', testMatrix)