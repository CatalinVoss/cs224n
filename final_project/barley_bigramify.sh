#!/bin/bash

#$ -N bigramify
#$ -o bigramify/barley_output/bigramify_out
#$ -e bigramify/barley_output/bigramify_error
#$ -cwd
#$ -S /bin/bash
	
	sh bigramify.sh 