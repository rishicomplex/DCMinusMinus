import sqlite3,os
def cycle(share,cur,original):
	try:
		list = os.listdir(share)
	except Exception:
		return cur
	for each in list:
		try:
			shape = share+'/'+each
			tell = shape[original:]
			if os.path.isdir(shape):
				cur.execute('insert into data values(NULL,"'+tell+'","-1KB",1)')
				cur = cycle(shape,cur,original)
			else:
				size=os.path.getsize(shape)
				if size >= 1048576:
					size = str(size/1048576) + 'MB'
				elif size >=1024:
					size = str(size/1024) + 'KB'
				else:
					size = str(size) + 'B'
				cur.execute('insert into data values(NULL,"'+tell+'","'+size+'",1)')
		except Exception:
			pass
	return cur

def run(share):
	dbc = sqlite3.connect(os.getcwd()+'\dc.db')
	cur = dbc.cursor()
	original=len(share)
	cur = cycle(share,cur,original)
	#cur.execute("select * from data order by hits desc")
	#rows=cur.fetchall()
	#for each in rows:
	#	print each
	dbc.commit()
	cur.close()