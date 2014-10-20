# Script to parse .gr files with syntactic sugar into one composite .grammar file to be fed into Jason Eisner's code.
# @author: Bharath Bhat (bbhat@stanford.edu)
# Last Modified: 10/14/2013

'''
This code converts grammar files written in syntactic sugar format into those expected by 
Eisner's C code base. It processes all .gr files in the directiry and outputs a .grammer file
which has all the grammar rules
'''
import sys,re,os


#working_directory = os.getcwd()
#grammar_files = ['S1.gr','S2.gr', 'Vocab.gr']
empty_string = 'e'
symbol_regex = re.compile(r'^\s*([^\(\)\{\}\|\s]+)\s*(.*)$')
empty_regex = re.compile(r'\s*\be\b(.*)$')
opt_regex = re.compile(r'^\s*\(.*\).*$')
alt_regex = re.compile(r'^\s*\{.*\}.*$')


def findEnclosing(expression,begin_char, end_char):
	begin = expression.find(begin_char)
	if (begin == -1):
		return (-1,-1)
	numOpen = 0
	strlen = len(expression)
	for i in range(begin,strlen):
		if expression[i] == begin_char:
			numOpen = numOpen+1
		elif expression[i] == end_char:
			numOpen = numOpen-1
		if numOpen == 0:
			return (begin,i)
	return (begin,-1)

def findParentheses(expression):
	return findEnclosing(expression,'(',')')

def findCurly(expression):
	return findEnclosing(expression,'{','}')

def handle_alts(alt_expression):
	if len(alt_expression)==0:
		return [[]]
	
	pipe_indx = alt_expression.find('|')
	if (pipe_indx == -1):
		return parse(alt_expression,[])
	expr = alt_expression[:pipe_indx]
	expr_expansions = parse(expr,[])
	remaining_alts = alt_expression[pipe_indx+1:]
	alt_expansions = handle_alts(remaining_alts)
	all_expansions = alt_expansions + expr_expansions
	return all_expansions

def parse(expression, symbols_so_far):
	expression = expression.strip()
	if len(symbols_so_far) == 0:
		symbols_so_far.append([])
	if len(expression)==0:
		return symbols_so_far	
	# Try Expanding Expression in order
	
	# Case 0: Special 'e' symbol
	m = re.match(empty_regex,expression)
	if (m != None):
		return parse(m.group(1),symbols_so_far)
		
	# Case 1: Simple rules of the form S -> NP VP
	m = re.match(symbol_regex,expression)
	if (m != None):
		sym = m.group(1)
		expr = m.group(2)
		for line in symbols_so_far:
			line.append(sym)
		return parse(expr,symbols_so_far)
	
	# Case 2: Handle Optionality: VP -> (V) VP
	paren_indices = findParentheses(expression)
	if (re.match(opt_regex,expression)!=None and paren_indices[0] != -1 and paren_indices[1] != -1):
		opt_expr = expression[paren_indices[0]+1:paren_indices[1]]
		expr = expression[paren_indices[1]+1:]
		opt_expansions = parse(opt_expr,[])
		new_lines = []
		for line in symbols_so_far:
			for exp in opt_expansions:
				line_with_option = line + exp
				new_lines.append(line_with_option)
		symbols_so_far = symbols_so_far + new_lines
		return parse(expr, symbols_so_far)

	# Case 3: Handle Alterations
	curly_indices = findCurly(expression)
	if (re.match(alt_regex,expression)!=None and curly_indices[0] != -1 and curly_indices[1] != -1):
		alt_expr = expression[curly_indices[0]+1 : curly_indices[1]]
		expr = expression[curly_indices[1]+1:]
		alt_expansions = handle_alts(alt_expr)
		new_lines = []
		for line in symbols_so_far:
			for alt_expansion in alt_expansions:
				new_line = line + alt_expansion
				new_lines.append(new_line)
		return parse(expr,new_lines)
	
	raise Exception('parseException', expression)

def main(working_directory):
	if not working_directory.endswith('/'):
		working_directory = working_directory + '/';
	grammar_files = [f for f in os.listdir(working_directory) if (os.path.isfile(os.path.join(working_directory,f)) and f.endswith('.gr'))]

	output_rules = []
	rule_map = {}

# Get all the rules in a rule_map dictionary first
	for grammar_file in grammar_files:
		f = open(os.path.join(working_directory,grammar_file),'r')
		lines = f.readlines()
		for line in lines:
			line_num = lines.index(line)
			line = line.strip()
			if (len(line)==0 or line[0] == '#'):
				continue;
		
			# first get weights
			splits = re.split(r'\s+',line,1)
			try:
				weight = float(splits[0])
			except:
				error_message = "Error parsing weight in line number " + str(line_num) + " in file: " + grammar_file + "; make sure you have a valid numeric weight."
				raise Exception(error_message)
			rule = splits[1]
		
			# get rule rhs and lhs
			rule_splits = re.split(r'\s*\->\s*',rule,1)
			lhs = rule_splits[0]
			try:
				rhs = rule_splits[1]
				rule_expansions = parse(rhs,[],)
			except Exception:
				error_message =  'Error on Line number ' + str(line_num) + ' in file ' + grammar_file
				raise Exception(error_message)
			if (lhs not in rule_map):
				rule_map[lhs] = []
			rule_map[lhs].append((weight,rule_expansions))
		f.close()

	for lhs in rule_map:
		for tup in rule_map[lhs]:
			num_rules = len(tup[1])
			for rule in tup[1]:
				output_rules.append((tup[0]/num_rules,lhs,rule))

	output_file = os.path.join(working_directory, 'processed_grammar.grammar');
	f = open(output_file,'w')
	for rule in output_rules:
		output_string = str(rule[0]) + '\t' + rule[1]
		for symbol in rule[2]:
			output_string = output_string + '\t' + symbol
		print >> f, output_string
	f.close()

if __name__ == "__main__":
	main(sys.argv[1]);
