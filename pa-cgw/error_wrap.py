#!/usr/bin/python

import os
import sys
import subprocess


def main(argv):
	found_error = False
	try:
		output = subprocess.check_output("check-for-new-terms -g *.grammar", stderr=subprocess.STDOUT, shell=True)
	except subprocess.CalledProcessError:
		print "Error checking for new terminals; make sure you set your bash PATH variable to include the pa-cgw directory, as outlined in the handout."
		return 1
	for line in output.splitlines():
		if "warning:  word" in line and "not in permitted vocabulary" in line:
			word = line.split(" ")[3]
			word = word[1:]
			word = word[:-1]
			find_word_location(word)
			found_error = True
	if found_error:
		return 1
	else:
		return 0

def find_word_location(word):
	for file in os.listdir("."):
		if len(file) > 3 and file[-3:] == ".gr":
			with open(file) as grammar_file:
				lines = grammar_file.readlines()
				for line in lines:
					line_num = lines.index(line) + 1
					if word in line.strip().replace("(", " ").replace(")"," ").replace("->", " ").replace("{", " ").replace("}", " ").replace("|", " ").split(" "):
						if file != "Vocab.gr":
							error_message = "Warning: you have included an invalid word (" + word + ") in grammar file: " + file + " on line " + str(line_num)
							error_message += "\nIf you meant to add a new POS tag, be sure to add it to Vocab.gr; non-terminals should have at least one production\n"
						else:
							error_message = "Warning: you have added an invalid word (" +word + ") to the list of terminals in Vocab.gr on line " + str(line_num) + "\n"
							error_message += "Please do not add any new terminals in this file\n"
						sys.stderr.write(error_message)
	 

sys.exit(main(sys.argv))
