/*******************************************************************/
Note : 
 - Run the program in the machines that are in same Sub-Network
 - Run the program in following machines 
   *	medusa.cs.rit.edu
   *	buddy.cs.rit.edu
   *	kansas.cs.rit.edu
   *	doors.cs.rit.edu
   *	yes.cs.rit.edu
   *	gorgon.cs.rit.ed
   *	idaho.cs.rit.edu
/*******************************************************************/

Step 1 : Copy all java files to the required folder 

Step 2 : Compile all java files. Command : javac *.java

step 3 : Run program using the Command : java Bully <ProcessID> 
		 Process ID for each machine is unique and Integer ID.
		 
		Note : Machine with highest process ID currently is the leader.
		
Step 4 : Press Ctrl-c in machine with highest process ID to start Election algorithm.

/********************************************************************/
sample Execution :
/********************************************************************/
Machine Name : medusa
java RicartAgravala 1

Machine Name : buddy
java RicartAgravala 2

Machine Name : doors
java RicartAgravala 3

Machine Name : yes
java RicartAgravala 4

Machine Name : kansas
java RicartAgravala 5

press Ctrl-c in machine "kansas" 
Wait for 10-15 seconds. Election starts in machines 
that detects the failure of Leader.

- Multiple machine may start the Election simultaneously.

- After all the elections are completed New leader process ID is printed.
  New Leader : 4

Now press Ctrl-c in machine "yes"
Wait for 10-15 seconds. Election starts in machines 
that detects the failure of Leader.

New Leader : 3 is printed.