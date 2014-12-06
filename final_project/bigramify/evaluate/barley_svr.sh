#!/bin/bash

#$ -N svr
#$ -o regressors/output
#$ -e regressors/error
#$ -cwd
#$ -S /bin/bash

./train_neural_network.py ${SGE_TASK_ID}