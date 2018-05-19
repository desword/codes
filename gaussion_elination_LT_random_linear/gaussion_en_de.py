#!/usr/bin/python

# source object - initial message
# object is broken down into Z >= 1 source blocks - target for a single raptor code application. identified by SBN
# blocks are broken into K source symbols. K is constant for a given object. each symbol has an ESI (encoding symbol identifier)
# size of source symbols: T. (so K*T = block size). 
# sub-blocks are made from EACH block, such that they can be decoded in working memory. N >= 1 subblocks. 
# sublocks have K sub-symbols, of size T'. 

import numpy
import random
import sys
import math
from bitarray import bitarray

# recommended alg values taken from RFC 5053 (basically represents how XOR
# operations are executed, 4 bytes at a time is standard on 32-bit, probably 8
# bytes on 64-bit? if XOR is done on single memory location)
A1 = 4
# min number of symbols per source block, in bytes
Kmin = 1024 
# target size for sub-block, in bytes. let's say 1kb
W = 1024










# one raptor manager is used per object
class RaptorManager:
	def __init__(self, filename, K=1024, debug=True):
		self.debug = debug
		self.f = open(filename, 'rb')
		# number of symbols in each source block
		self.K = int(K)
		# keep a counter of how many blocks get sent out. 
		self.current_block = 0
		# remember how much padding the last block used 
		self.padding_last = None
		self.last_block = False
		# constraint matrix for codes that use pre-coding.
		self.G = None
		if self.debug:
			print "registered " +self.f.name+ "."
	
	def _encode_binary_block(self):
		# encode a chunk of size k from the input file as binary and return

		# the file read() method reads in 1-byte (8 bit) chunks, so make sure K
		# is a multiple of 8, and then divide K by 8 to determine how many bits
		# to read, to keep num symbols (bits) equal to  K. 
		if self.K % 8 != 0:
			sys.stderr.write("error: K must be a byte multiple (ie multiple of 8).")
			return
		n = self.K/8

		block = bitarray()
		start_pos = self.f.tell()
		try:
			block.fromfile(self.f,n)
		except EOFError:
			# if we reach the end of the file, get just the remaining bytes and
			# pad the block as necessary
			end_pos = self.f.tell()
			print "started at post %d, reached EOF at pos %d" % (start_pos, end_pos)
			remaining_bytes = end_pos - start_pos
			self.f.seek(start_pos)
			block = bitarray()
			block.fromfile(self.f,remaining_bytes)
			padding = n - remaining_bytes
			self.padding_last = padding
			print "remaining bytes: %d, of required %d bits or %d bytes. current length = %d, ==> padding = %d" % (remaining_bytes, self.K, self.K/8, len(block), padding)
			self.last_block = True
			if padding*8 == self.K:
				return None
			else:
				block.extend('0'*padding*8)
		print "length of block is %d" % len(block)
		print "self.K = %d" % self.K
		assert len(block) == self.K
		return block
	
	def generate_constraint_matrix(self, c, d):
		# c is the number of constraint symbols.
		# d is the density (remember LDPC is LOW density)
		# construct a k x c matrix. because c is a constant and not from a
		# distribution, this is considered a "regular" LDPC code. 
		assert (0 < d < 1)
		# given the size k x c, calculate the number of bits to make into 1s
		# note the each of the c rows of the matrix G will form the (n-1)
		# coefficients of a constraint symbol, where n is the number of ones
		# present in the column, calculated as the product of the density term
		# d and k. 
		num_ones = int(c*self.K*d)
		# build G as a long vector at first so it's easier to address specific
		# bits. we'll reshape it below. 
		redundant = True
		while redundant:	
			G = numpy.zeros(self.K*c, int)
			one_indices = random.sample(xrange(self.K*c), num_ones)
			G[one_indices] = 1
			G.shape = (self.K,c)
			if numpy.linalg.matrix_rank(G) == c:
				redundant = False

		print "G matrix:"
		print G
		# both the encoder and deocder need to know G
		self.G = G
		return G

	def get_constraint_matrix(self):
		return self.G

	def num_bits(self, block):
		# block is a bitarray object, which packs bits into longs. so number of
		# bits is the size in bytes * 8. 
		info = block.buffer_info()
		return info[1]*8

	def next_block(self):
		# keep track of where we are and return the next block
		if self.last_block:
			return None
		the_block = self._encode_binary_block()
		self.current_block += 1
		return the_block

class RaptorEncoder:
	def __init__(self, block, G=None, symb_size=1, debug=True):
		# precode and distribution are each function variables
		self.debug = debug
		# symbols is a bitarray()
		self.symbols = block
		# generator matrix for pre-code, if any
		self.G = G
		# precoded is a bitarray()
		self.intermediate = None

	def ldpc_precode(self):
		# constraint matrix self.G must exist and be passed in as an
		# initialization argument to the encoder. 

		# convert symbols to a numpy array for matrix multiplication
		G_rows, G_cols = self.G.shape
		k = numpy.array(self.symbols.tolist())
		print "precoding with generator matrix..."
		print self.G
		print "source symbols"
		print self.symbols
		# now here is the key: we must calculate the c redundant symbols z_i
		# such that z_i xor G[:,i] = 0. 
		z = numpy.array([], int)
		for i in xrange(G_cols):
			# sum([a,b,c...]) % 2 is equivalent to a^b^c^... 
			coefficients = self.G[:,i]
			print "coefficients"
			print coefficients
			other_terms = coefficients*k
			print "other terms"
			print other_terms
			xor_other_terms = sum(other_terms) % 2
			print "xor value of other terms: %d" % xor_other_terms
			# we require xor_other_terms ^ zi = 0. this is true when zi has the
			# same value as xor_other_terms. 
			z = numpy.append(z, xor_other_terms)

		# store as bitarray
		self.z = bitarray(z.tolist())
		self.intermediate = self.symbols + self.z
		print "all intermediate symbols:"
		print self.intermediate
		print len(self.intermediate)
		assert len(self.intermediate) == G_cols+len(k)
		return self.intermediate
	
	def distribution_random_LT(self, num_symbols):
		# return a vector of coefficient indices sampled from the contained
		# distribution. example return value: [17,22,238]







		# sample a weight from uniform distribution
		d = numpy.random.random_integers(1, num_symbols)

		# construct a vector of d coefficient indices from k. (sampling without
		# replacement. 
		v = random.sample(range(num_symbols),d)
		return v


	def LT_RobustSoliton_genRho(self, num_symbols):

		k = num_symbols
		rho_set = []
		rho_set.append(1.0/k)
		for i in range(2, k+1):
			rho_set.append(1.0/ ((i-1)*i))
		return rho_set
		
	def LT_RobustSoliton_genTau(self, s, num_symbols, delta):

		k = num_symbols
		tau_set = []

		pivot = (int) (k*1.0/ s)

		for i in range(1, k+1):
			if i < pivot:
				tau_set.append( s*1.0 / k * 1/ i)
			elif i == pivot:
				tau_set.append( s*1.0 / k * math.log(s *1.0/delta))
			else:
				tau_set.append(0)
		return tau_set
		

	def LT_RobustSoliton_genMu(self, num_symbols, delta, c):

		mu_set = []
		k = num_symbols
		s = c * math.log(k*1.0/ delta) * math.sqrt(k)

		rho_set = self.LT_RobustSoliton_genRho(num_symbols)
		tau_set = self.LT_RobustSoliton_genTau(s, num_symbols, delta)

		normalizer = sum(rho_set) + sum(tau_set)

		for i in range(len(tau_set)):
			mu_set.append( (rho_set[i] +  tau_set[i]) /normalizer )

		return mu_set
		


	def LT_RobustSoliton_genRsd(self, num_symbols, delta, c):

		rsd_set = []

		mu_set = self.LT_RobustSoliton_genMu(num_symbols, delta, c)

		tmpsum = mu_set[0]
		rsd_set.append(tmpsum)
		for i in range( 1, len(mu_set)):
			tmpsum = tmpsum + mu_set[i] 
			rsd_set.append(tmpsum)
		return rsd_set

		


	def get_sample_degree(self, cdf_p):

		p = random.random()

		index = 0;
		size = len(cdf_p)
		while index < size:
			if cdf_p[index] > p:
				return index + 1
			index = index +1
		return index + 1

		

	def synapse_distribution(self, num_symbols):
		global blockC

		global delta
		global c



		# synapse dsitribution, 32 blocks
		# cdf_p = [0.027, 0.057,0.089,0.1212,0.1536,0.18627,0.21894,0.25161,0.28428,0.31695,0.34962,0.38229,0.41496,0.44763,0.4803,0.51297,0.54564,0.57831,0.61098,0.64365,0.67632,0.70899,0.74166,0.77433,0.807,0.83967,0.87234,0.90501,0.93821,0.97021,0.99541,1.00051]

		# ### LT_robust distirubtion for fixed c and delta parameters.
		# if blockC == 32:
		# 	cdf_p = [0.03189004075060693, 0.5307690923554182, 0.6971842773033112, 0.7804529536504023, 0.8304508097825438, 0.863807147419896, 0.8876505554103317, 0.9055462008045466, 0.9194752167566823, 0.9306265740348102, 0.9397570755030761, 0.9473713798060077, 0.9538189514371918, 0.9593494689148975, 0.9641460745692793, 0.9683461587105205, 0.9720548101824898, 0.9753537847146905, 0.9783076420636119, 0.9809680426420563, 0.9833768169903577, 0.9855681984465576, 0.9875704736229254, 0.9894072204448815, 0.9910982491985441, 0.9926603265195829, 0.9941077385782054, 0.9954527336465001, 0.9967058731515598, 0.9978763125591301, 0.9989720279258656, 1.0000000000000004]
		# elif blockC == 24:
		# # 24 blocks
		# 	cdf_p = [0.04233056702559763, 0.5412370410251359, 0.7076705081598752, 0.7909528962946915, 0.8409617219160492, 0.874327200823933, 0.8981784442059776, 0.9160809455783925, 0.9300160557404007, 0.9411728978123335, 0.9503083854603636, 0.9579272604307626, 0.964379051141673, 0.9699134863378894, 0.9747137485308454, 0.9789172606780542, 0.9826291385094493, 0.9859311601596089, 0.9888879042524691, 0.9915510472381731, 0.9939624334033562, 0.9961563079578719, 0.9981609678373099, 1.0]
		
		# # 16 blocks
		# elif blockC == 16:
		# 	cdf_p = [0.0631813644836315, 0.562153004490242, 0.7286208684004998, 0.8119267923096558, 0.8619535418275657, 0.8953335049544497, 0.919196904889089, 0.93710988168736, 0.9510541956339086, 0.962219245718351, 0.9713621403642433, 0.9789877639589135, 0.9854457525354827, 0.9909859180376428, 0.995791508632126, 1.0]
		# elif blockC == 8:
		# # 8 blocks
		# 	cdf_p = [0.12564551316662537, 0.6248057786618335, 0.7913554760731549, 0.8747117959019417, 0.9247744704730894, 0.9581821753031049, 0.9820680990740092, 1.0]

		### LT_robust distirubtion generated distriubtion considering different parameters.
		cdf_p = self.LT_RobustSoliton_genRsd(num_symbols, delta, c)



		degree = self.get_sample_degree(cdf_p)

		v = random.sample(range(num_symbols),degree)

		return v

	def generate_encoded(self):
		if self.intermediate:
			symbols = self.intermediate
		else:
			symbols = self.symbols

		# the distribution fn must take as argument the number of symbols it is
		# operating over, and return a vector of coefficients indices, sampled
		# according to the distribution therein.  example output for 10
		# precoded symbols: [2,4,9]

		# v = self.distribution_random_LT(len(symbols))
		v = self.synapse_distribution(len(symbols))
		v.sort()

		# grab the symbols at the index positions that have a 1 in the
		# coefficient vector.
		selected_symbols = [symbols[idx] for idx in v]

		# get the first two values to start the xor process (note this actually
		# removes the values from selected_symbols, so after the for loop below
		# selected_symbols will be in a wierd state and should not be used. 
		xorval = sum(selected_symbols) % 2
			
		# return the xor'ed value and the associated coefficients
		return {'val': xorval, 'coefficients': v}

class RaptorGaussDecoder:

	def __init__(self, K, debug=True):
		self.debug = debug
		self.K = K
		self.A = numpy.array([], dtype=bool)
		self.b = numpy.array([], dtype=bool)
		self.blocks_received = 0
		self.blocks_processed = 0

	def add_block(self, encoded):
		# increment number of blocks received either way
		self.blocks_received += 1

		val = bool(encoded['val'])
		coeff = encoded['coefficients']
		
		# create a new row vector and set it to one as indicated by the
		# coefficient vector
		# [cgl]-- get the block xor index of the received encoded blocks.
		new_row = bitarray('0'*self.K)
		for i in range(self.K):
			if i in coeff:
				new_row[i] = 1
			if i > max(coeff):
				break
		print "encoded block: " + str(new_row)

		# compare it to the ones we've already received:
		# [cgl] add the received row only when there is no duplicated row in the current concated matrix A.
		duplicate = False
		for row in self.A:
			if numpy.all(row == new_row):
				duplicate = True
				break


		# [cgl] if the received xor combination is new, then add into the matrix A.
		if not duplicate:
			# add the new row to the bottom
			if not len(self.A):
				self.A = numpy.array(new_row.tolist())
			else:
				self.A = numpy.vstack((self.A, numpy.array(new_row.tolist())))
			self.b = numpy.append(self.b, val)

	def is_full_rank(self):
		if self.A.size == 0:
			return False

		# b = numpy.array([self.b])
		# print "b", b
		# print "A", self.A
		# both = numpy.hstack((self.A,b.T)) # why is combined the results with the block xor index Matrix.
		# rank_ab = numpy.linalg.matrix_rank(both)
		rank_a = numpy.linalg.matrix_rank(self.A)

		# print "===========check full rank"
		# print "b", b
		# print "A+b.T", both
		# print "Rank_ab", rank_ab
		# print "Rankd_a", rank_a


		'''
		# print out list versions to copy over to matlab for sanity checking.
		A = self.A.copy()
		A = A.tolist()
		intA = []
		for row in A:
			intArow = [int(a) for a in row]
			intA.append(intArow)
		print intA
		intb = [int(bb) for bb in self.b.tolist()]
		print intb
		'''
		# if rank_ab == rank_a and rank_ab == self.K:
		if self.K == rank_a:

			# print "rank_ab = rank_a = %d" % rank_ab
			print 'self.K', self.K, 'A', rank_a
			print "matrix is full rank."
			return True
		# print "rank_ab = %d, != rank_a = %d" % (rank_ab, rank_a)
		print "K = %d, != rank_a = %d" % (self.K, rank_a)
		return False

	def num_blocks(self):
		# how many encoded blocks have we received so far?
		# this is equivalent to the numer of rows in A. shape() returns (rows, cols)
		return self.A.shape[0]

	def remove_null_rows(self, mat):
		# empty rows
		rows, cols = mat.shape
		all_false = numpy.array([], int)
		for r in xrange(rows):
			if not mat[r,:].any():
				all_false = numpy.append(all_false, r)
				
		to_keep = [r for r in xrange(rows) if r not in all_false]
		return mat[to_keep,:]

	def remove_duplicate_rows(self, mat):
		# duplicates
		duplicates = numpy.array([], int)
		rows, cols = mat.shape
		for r in xrange(rows):
			this_row = mat[r,:]
			for rr in xrange(r+1,rows):
				if rr in duplicates:
					continue
				test_row = mat[rr,:]
				diff = test_row ^ this_row
				if not numpy.any(diff):
					duplicates = numpy.append(duplicates, rr)
		to_keep = [r for r in xrange(rows) if r not in duplicates]

		return mat[to_keep,:]


	def decode_gauss_base2(self):
		# use tmp matrices in case our solution fails. 
		b = numpy.array([self.b])
		print b.shape
		print self.A.shape
		mat = numpy.hstack((self.A,b.T))
		tri, b = self._triangularize(mat)
		if tri == None:
			return None
		return self._backsub(tri, b)

	def _backsub(self, tri, b):
		mat = numpy.hstack((tri, numpy.array([b]).T))
		rows, cols = tri.shape
		soln = numpy.zeros(cols, int)
		# initialize solution vector with RHS of last row
		for i in (xrange(cols)).__reversed__():
			# the second term is always a 1 since the row in question always
			# has a 1 at the diagonal
			inner_term = (numpy.dot(mat[i,i:cols],soln[i:cols]) + mat[i,-1]) % 2
			soln[i] = numpy.logical_and(inner_term, 1)
			#print "soln for x" + str(i)+" = " + str(soln[i])
		
		return soln

	def _triangularize(self, mat):

		#mat = self.remove_null_rows(mat)
		#mat = self.remove_duplicate_rows(mat)
		rows, cols = mat.shape
		# we tacked the solution vector onto the end of the matrix, so don't
		# count it in terms of the number of columns to iterate over. 
		cols = cols -1
		
		# print 'mat', mat
		# print 'rows', rows, 'cols', cols

		# first, we want to pivot the rows to put A in upper triangular form
		# (get 0's into all columns positions below the given row)
		for c in xrange(0, cols):
			# examine the row values below the diagonal (note that c starts at
			# 1, not 0. so in the first column, go from 1 to rows, in the
			# second column, go from 2.. rows, etc)
			col_vals = mat[c:rows,c]
			# print '[c:rows, c]', c, rows, c
			# print 'col_vals', col_vals
			if col_vals.max() == 0:
				print "error: all zeros below row/column (%d, %d). multiple solutions." % (c,c)
				# print mat[c:rows, c:rows]
				return [None, None]
			# find first row with a 1 in the left-most column (non-zero returns
			# a tuple, and we want the 0'th element of the first dimension of
			# the tuple since this is just a row vector)
			max_i = col_vals.nonzero()[0][0]

			# do the 'partial pivot': swap rows max_r and c in A and b (unless
			# the current row already has a one, then we're not going to get
			# any better). 
			if not (max_i+c) == c:
				upper_row = mat[c,:].copy()
				lower_row = mat[c+max_i,:].copy()
				mat[c,:] = lower_row
				mat[c+max_i,:] = upper_row

			# now zero out the 1's remaining in this column below the diagonal.
			# get the c'th row (yes, c is also the column value - this ensures
			# we start at the row below the diagonal)
			cth_row = mat[c,:]

			# now for all rows below this one, xor the c'th row with those that
			# contain a 1 in this column, in order to make it 0. (make sure to
			# do this with the right hand solution vector, too). 
			for r in xrange(c+1,rows):
				if mat[r,c] == 1:
					mat[r,:] = (cth_row ^ mat[r,:])
		# end column iteration

		# now we can get rid of the dangling rows since our solution is
		# uniquely specified by the top square component. 
		mat = mat[0:cols,:]

		return mat[:, 0:cols], mat[:, -1]


	def decode_gauss_base10(self):
		# attempt decode
		print "attempting solution..."
		if not self.is_full_rank():
			print "A is not full rank, sorry."
			return None
		soln, residues, rank, sing = numpy.linalg.lstsq(self.A, self.b)
		self.decoded_values = soln 
		return soln, residues, rank, sing

	def convert(self):
		# convert the values back to strings
		bits = bitarray(self.decoded_values.tolist())
		return bits.tostring()
		

def run_gauss(filename):

	global fp
	global blockC
	global prr

	global c
	global delta


	DEBUG = True

	TO_list = [] # record the transmission overhead



	# if we want everything to go in one block, then use len(data) as the block
	# length
	# K = 16
	K = blockC
	# epsilon = int(0.5*8)
	epsilon = 1
	manager = RaptorManager(filename, K)
	block = manager.next_block()
	decoded_blocks = []
	processed_blocks = 0
	output_blocks = 0
	while block:
		output_blocks += 1
		print "-----------------------------"
		print "block %d" % output_blocks
		if DEBUG: 
			print "encoding original (source) block: " + str(block)
		# this encoder is non-systematic and uses no pre-code. 
		encoder = RaptorEncoder(block)
		decoder = RaptorGaussDecoder(K)

		# grab new symbols and periodically check to see if we've gathered enough
		# to find a solution. when the decoder matrix is full rank, try and solve
		# for the original symbols. 

		extra_needed_blocks = 0

		while True:
			while True:
				e = encoder.generate_encoded()
				if random.random() < prr:
					break;
				extra_needed_blocks = extra_needed_blocks + 1


			decoder.add_block(e)
			if decoder.is_full_rank():
				original_block = decoder.decode_gauss_base2()
				if original_block != None:
					break;

		# while not decoder.is_full_rank():
		# 	for i in xrange(epsilon+1):
		# 		e = encoder.generate_encoded()
		# 		decoder.add_block(e)

		print "attempting to solve after " + str(decoder.blocks_received) + " blocks."
		original_block = decoder.decode_gauss_base2()

		# strB = [str(i) for i in original_block]
		# strGet = ''.join(strB)
		# print "The alpha is :", chr(int(strGet,2))

		print "decoded block was..."
		print original_block


		decoded_blocks.append(original_block)
		processed_blocks += decoder.blocks_processed
		block = manager.next_block()


		txBLockC = decoder.blocks_received + extra_needed_blocks
		recBLockC = blockC
		TO_list.append(txBLockC*1.0/recBLockC)



	# print "decoder processed %d output blocks after %d received blocks. A total of factor of %d average overhead" % (output_blocks, processed_blocks, processed_blocks/float(output_blocks))
	# print "decoded blocks:"
	# print decoded_blocks
	# print "decoded message"
	# for d in decoded_blocks:
	# 	strB = [str(i) for i in d]
	# 	strGet = ''.join(strB)
	# 	print chr(int(strGet,2)),
	# 	# print "The alpha is :", chr(int(strGet,2))
	# 	# sys.stdout.write(d.tostring())
	# print ""

	# outStr = "The over head for prr:%f, blockC:%d, is TO:%f"  % (prr, blockC, sum(TO_list)/len(TO_list))
	# print "The over head for prr:%f, blockC:%d, is TO:%f"  % (prr, blockC, sum(TO_list)/len(TO_list)) 

	outStr = "The over head for prr:%f, blockC:%d, is TO:%f. with parameter C:%f, delta:%f "  % (prr, blockC, sum(TO_list)/len(TO_list), c, delta)
	print outStr
	fp.write(outStr)
	return sum(TO_list)/len(TO_list)

	
if __name__ == '__main__':
	global fp
	global blockC
	global prr

	global delta
	global c

	filename = "send.txt"

	outputFileName = ""


	# prrSet = [0.5, 0.6, 0.7, 0.8, 0.9, 0.95]
	prrSet = [0.9]
	# blockCout = [8, 16, 24, 32] #  Define the nubmer of blocks in one chunck that need to be decoded together. In current setting, the packet chunk is 8 bit and 1 bit for each block. 
	# blockCout = [32] # currently the distribution is for block count 32 the best.

	blockCout = [8, 16, 24, 32]


	cSet = [0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1, 0.5]
	# deltaSet = [ 0.01, 0.1 , 0.2, 0.5, 0.7 , 0.9]
	deltaSet = [0.0001, 0.0005, 0.001, 0.005 ]

	for prr in prrSet:
		for blockC in blockCout:
			outputFileName = "log/prr" + str(prr) + "_bc" + str(blockC) + ".txt"
			fp = open(outputFileName, 'w')

			MinTo = 999
			bestC = 0
			bestDelta = 0

			for c in cSet:
				for delta in deltaSet:
					tmpTo = run_gauss(filename)

					if tmpTo < MinTo:
						MinTo = tmpTo
						bestC = c
						bestDelta = delta

			outs = "Best paramet, c:%f, dealt:%f, To:%f "  % (bestC, bestDelta, MinTo)
			fp.write(outs)
			fp.close()



	




