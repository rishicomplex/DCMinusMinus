x = raw_input("Please enter the folder path to index \n")
import create_database,table_edits
create_database.datamake(x)
table_edits.run(x)