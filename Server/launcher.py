import sys,os,py_compile
#hoping that pythonw.exe is in same directory as python.exe
loc = sys.executable
if loc.find('python.exe'):
	loc=loc.replace('python.exe','pythonw.exe')
	try:
		py_compile.compile('webserver.py')
	except:
		pass
print "You May Close This Window."
os.system(loc+' '+os.getcwd()+'\webserver.pyc')
sys.exit()