#!/usr/bin/env python

f = open('dictionary.txt')
for line in f:
	entry = line.split()
	if len(entry) <= 1 or "." in entry[1] or "(a" in entry[1] or "(us" in entry[1] or "-" in entry[0] or "see" in entry[1] or "usage" in entry[0] or "Usage" in entry[0] or "slang" in entry[1] or "archaic" in entry[1] or "&" in entry[1]:
		continue
	word1 = entry[0].lower().replace("\'", "").replace(",", "");
	word2 = entry[1].lower().replace("\'", "").replace(",", "");
	if len(word1) == 0 or len(word2) == 0:
		continue
	print word1, word2
