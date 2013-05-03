import sqlite3,os
def datamake(x):
	dbc = sqlite3.connect(os.getcwd()+'/dc.db')
	cur = dbc.cursor()
	try:
		cur.execute("drop table data")
	except Exception:
		pass
	cur.execute("create table data (serial INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, address text, size text, hits INTEGER NOT NULL)")
	log = open('settings.dat','w')
	log.write(x)
	log.close()
	cur.close()