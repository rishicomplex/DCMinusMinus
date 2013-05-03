from bottle import route,request,run
from pyftpdlib import ftpserver
from threading import Thread
from ftplib import FTP
import os
import sqlite3
import cherrypy
fport=4007
sport=9090
shost="10.136.77.130"
ip='127.0.0.1'
user="music"
passw="91214"
dir = os.getcwd()
file = open('core_files/settings.dat','r')
share = file.read()
file.close()
os.chdir(share)
if not os.path.exists("uploads"):
    os.makedirs("uploads")
def fconnect(ip,port):
	fcon = FTP()
	fcon.connect(ip,port)
	fcon.login(user,passw)
	return fcon
def sconnect():
	dbc = sqlite3.connect(dir+'\core_files\dc.db')
	cur = dbc.cursor()
	return cur
@route('/')
def index():
	return 'error man'
@route('/:name',method='post')
def postcheck(name=''):
	if name == 'viewer':
		code = request.POST.get('code')
		key = request.POST.get('key')
		if not code:
			return ""
		else:
			if code == "91214":
				#fcon = fconnect(ip,port)
				#fcon.sendcmd("TYPE i")
				#try:
				#	size = fcon.size(key)
				#except Exception:
				check = os.path.isdir(share+key)
				if check:
					cur = sconnect()
					count = str(key.count("/"))
					cur.execute("select * from data where address like '"+key+"%' and length(address)-length(replace(address,'/',''))="+count)
					rows=cur.fetchall()
					cur.close()
					sum = ""
					for each in rows:
						sum = sum + each[1]+"/<size>"+each[2]+"<br/>"
					return sum
				else:
					return "lotteryaadirishsree"
			else:
				return ""
	elif name == 'downloads':
		code = request.POST.get('code')
		key = request.POST.get('key')
		if not code:
			return ""
		else:
			if code == "91214":
				dbc = sqlite3.connect(dir+'\core_files\dc.db')
				cur = dbc.cursor()
				cur.execute("update data set hits=hits+1 where address='"+key+"'")
				dbc.commit()
				cur.close()
			else:
				return ""
	elif name == 'uploads':
		code = request.POST.get('code')
		key = request.POST.get('key')
		if not code:
			return ""
		else:
			if code == "91214":
				dbc = sqlite3.connect(dir+'\core_files\dc.db')
				cur = dbc.cursor()
				size = os.path.getsize(share+'/uploads/'+key)
				if size >= 1048576:
					size = str(size/1048576) + 'MB'
				elif size >= 1024:
					size = str(size/1024) + 'KB'
				else:
					size = str(size) + 'B'
				cur.execute('insert into data values(NULL,"/uploads/'+key+'","'+size+'",2)')
				dbc.commit()
				cur.close()
	else:
		return 'sorry'
@route('/:name',method='get')
def getcheck(name=''):
	if name == 'finder':
		cmda=request.GET.get('key')
		if not cmda:
			return 'error'
		else:
			cur = sconnect()
			cmd=cmda.split(' ')
			cmd="%".join(cmd)
			cur.execute("select * from data where address like '%"+cmd+"%' and size != '-1KB' order by hits desc")
			rows=cur.fetchall()
			cur.close()
			sum=""
			sum2=""
			for each in rows:
				if each[1].find(cmda) != -1:
					sum=sum+each[1]+"<size>"+each[2]+"<br/>"
				else:
					sum2=sum2+each[1]+"<size>"+each[2]+"<br/>"
			sum=sum+sum2
			return sum
	elif name == 'ads':
		ads=open(dir+'/ads.txt','r')
		x = ads.read()
		ads.close()
		x=x.replace("\\5","\5")
		return x
	else:
		return 'sorry'
def runWebServer():
	run(host = shost,port = sport,server = 'cherrypy')
one = Thread(target = runWebServer,args = ())
one.start()
authorizer = ftpserver.DummyAuthorizer()
authorizer.add_user(user, passw, share, perm='elr')
authorizer.override_perm(user, share+'/uploads','elrw')
handler = ftpserver.FTPHandler
handler.authorizer = authorizer
address = (shost, fport)
ftpd = ftpserver.FTPServer(address, handler)
ftpd.serve_forever()