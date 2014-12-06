#!/bin/bash

#$ -N bigram_sentences
#$ -o barley_output/build_bigrams_out
#$ -e barley_output/build_bigrams_error
#$ -cwd
#$ -S /bin/bash

./bigram_construct_matrices.py 