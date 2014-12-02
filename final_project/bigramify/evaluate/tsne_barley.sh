#!/bin/bash

#$ -N tsne
#$ -o barley_output/tsne_out
#$ -e barley_output/tsne_error
#$ -cwd
#$ -S /bin/bash
	
	./run_tsne.py 