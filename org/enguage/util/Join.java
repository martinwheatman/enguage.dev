package org.enguage.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import org.enguage.object.Attribute;
import org.enguage.object.Attributes;

import org.enguage.util.Audit;
import org.enguage.util.Strings;

public class Join {
	private static       Audit           audit = new Audit( "Join" );
	
	static private boolean on = true;
	static public  void    on( boolean j ) { on = j; }
	
	static private ArrayList<ArrayList<Integer>> combinations( ArrayList<Integer> dimensions ) {
		//if (Audit.detailedDebug) audit.in( "combinations", dimensions.toString());
		/* If we save as OBJECT.0, ... OBJECT.n need to do a join on SUBJECT.m needs OBJECT.n
		 *   e.g. martin and james need a beer and a packet of crisps.
		 *        martin needs a beer.
		 *        james  needs a beer.
		 *        martin needs a packet of crisps.
		 *        james  needs a packet of crisps.
		 *      
		 */
		/* fn() => int sizes[] -> int[] offsets[]
		 *   [1,1] => [0,0];
		 *   [2,1] => [0,0], [1,0];
		 *   [1,2] => [0,0], [0,1];
		 *   [3,1] => [0,0], [1,0], [2,0];
		 *   
		 *   [3,2] => [0,0]; -- initial value
		 *         => [0,0], [0,0], [0,0]; -- copy initial value dimension times
		 *         => [0,0], [1,0], [2,0]; -- adjust 0th dimension values
		 *         => [0,0], [1,0], [2,0], [0,0], [1,0], [2,0]; -- propagate 0th dimension, 1st dimension times
		 *         => [0,0], [1,1], [2,0], [0,1], [1,0], [2,1]; -- adjust 1st dimension
		 *   [2,3] => [0,0], [1,1], [0,2], [1,0], [0,1], [1,2];
		 * [1,2,1] => [0,0,0], [0,1,0]
		 */
		ArrayList<ArrayList<Integer>> rc = new ArrayList<ArrayList<Integer>>();
		{	//initialise rc with blank zeros, size of dimensions array, e.g. [2, 1] => [0, 0]
			ArrayList<Integer> b = new ArrayList<Integer>();
			for (int i=0, dsz = dimensions.size(); i<dsz; i++) b.add( 0 );
			rc.add( b );
		}
		
		// loop through all the dimensions, maintaining an index...
		int dimensionIndex = -1;
		ListIterator<Integer> ai = dimensions.listIterator();
		while (ai.hasNext()) {
			int dimension = ai.next();
			dimensionIndex++;
			
			// ...adding rc into itself for each dimension, and setting that dimension
			// by copying, this will propagate earlier values set in rc....
			ArrayList<ArrayList<Integer>> tmp = new ArrayList<ArrayList<Integer>>();
			for (int j=0; j<dimension; j++) {
				// set this dimension's values
				ListIterator<ArrayList<Integer>> ri = rc.listIterator();
				while (ri.hasNext()) {
					ArrayList<Integer> li = new ArrayList<Integer>( ri.next());
					li.set( dimensionIndex, j );
					ri.set( li );
				}
				//add this into rc
				tmp.addAll( rc );
			}
			rc = tmp;
		}
		//if (Audit.detailedDebug) audit.out( rc.toString() );
		return rc;
	}
	static public ArrayList<Attributes> join( Attributes match, Strings sep ){
		ArrayList<Attributes> rc = new ArrayList<Attributes>();
		if (!on) {
			audit.log( "join returning:"+ match.toString());
			rc.add( match );
		} else {
			/* there is probably a much easier way to do this!
			 * -- pass values into combinations() and annotate there!
			 */
			//if (Audit.detailedDebug) audit.in( "join", "["+ match +"], '"+ sep +"'" );
			
			// first we get loaded=["SUBJECTS","OBJECTS"]
			Strings names = match.names();
			//if (Audit.detailedDebug) audit.log( "loaded are: "+ names.toString() );
			
			ArrayList<ArrayList<Strings>> values = match.valuesAsLists( sep );
			
			//if (Audit.detailedDebug) audit.log( "values are: "+ values.toString() );
			
			ArrayList<Integer> dimensions = new ArrayList<Integer>();
			/* Here we have raw arrays, of values and loaded. To limit the join combinations,
			 * without doing anything too smart, limit dimensions to loaded which appear plural.
			 * This puts into the users hands which values are joined.
			 */
			for (ArrayList<Strings> value : values)
				dimensions.add( value.size());
			ArrayList<ArrayList<Integer>> numbers = combinations( new ArrayList<Integer>( dimensions ));
			
			Iterator<ArrayList<Integer>> nui = numbers.iterator();
			while (nui.hasNext()) { // looping round numbers [0, 0]
				ArrayList<Integer> number = nui.next(); // 0, 0
				//if (Audit.detailedDebug) audit.log( "Numbers are:"+ number.toString() );
				Attributes runs = new Attributes();
				//
				int vi=0; // index into values
				Iterator<String>  ni = names.iterator();
				Iterator<Integer> li = number.iterator();
				while (li.hasNext()) { // looping round name/number combo
					int    lii = li.next();
					String nii = ni.next();
					Attribute a = new Attribute( nii, values.get( vi ).get( lii ).toString( Strings.SPACED ));
					runs.add( a );
					vi++;
				}
				rc.add( runs );
		}	}
		//if (Audit.detailedDebug) audit.out( rc.toString() );
		return rc;
	}

	public static void main( String argv []) {
		Audit.allOn();
		
		Attributes match = new Attributes();
		match.add( new Attribute( "SUBJECT", "martin" ));
		match.add( new Attribute(  "OBJECT", "2 coffees, a pot of tea and biscuits" ));
		
		audit.log( "Context is:\n\t"+ match );
		ArrayList<Attributes> ala = join( match, new Strings( ", and" ));
		audit.log( "Combinations of which are:\n\t"+ ala.toString() );
		
		match = new Attributes();
		match.add( new Attribute( "SUBJECTS", "martin" ));
		match.add( new Attribute(  "OBJECTS", "2 coffees, a pot of tea and biscuits" ));
		
		audit.log( "Context is:\n\t"+ match );
		ala = join( match, new Strings( ", and" ));
		audit.log( "Combinations of which are:\n\t"+ ala.toString() );
}	}
