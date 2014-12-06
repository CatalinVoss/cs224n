#!/usr/bin/env python

import numpy as np
import cPickle


regressors = []
for i in range(300):
	print 'Opening file for regressor', i
	with open('regressors2m/regressors_' + str(i) + '.pkl', 'rb') as f:
		regressor = cPickle.load(f)
	regressors.append(regressor)

print "Finished... Writing file"
with open('regressors2m_big.pkl', 'wb') as f:
	cPickle.dump(regressors, f)