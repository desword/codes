

sendLen = 3000 # byte

f = open("send.txt",'w')


for i in range(sendLen):

	inpu = chr(ord('a') + (i % 26))

	# if i % 26 == 0 and not i == 0:
	# 	inpu = '\na'
	# else:
	# 	inpu = chr(ord('a') + (i % 26))

	f.write(inpu)

f.close()












