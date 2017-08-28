package com.yagadi.enguage.interpretant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import com.yagadi.enguage.object.Spatial;
import com.yagadi.enguage.object.Temporal;
import com.yagadi.enguage.util.Audit;
import com.yagadi.enguage.util.Strings;
import com.yagadi.enguage.vehicle.Reply;

public class Sign {
	
	public Tag pattern = new Tag(); // we're just interested in contents...
	
	private static final String   NAME = "sign";
	private static       Audit   audit = new Audit( NAME );
	private static final String indent = "    ";

	public Sign() {
		super();
		pattern.name( NAME );
	}
	public Sign( String concept ) {
		this();
		concept( concept );
	}
	
	ArrayList<Intention> intentions = new ArrayList<Intention>();
	public ArrayList<Intention> intentions() { return intentions; }
	public Sign intention( int posn, Intention intent ) { intentions.add( posn, intent ); return this;}
	//public Sign create( Intention intent )
	public Sign append( Intention intent ){ intentions.add( intent ); return this;}
	public Sign headAppend( Intention intent ){ intentions.add( 1, intent ); return this;}
	public Sign prepend( Intention i ){ intentions.add( 0, i ); return this;}
	public Sign add( int i, Intention intent ) { intentions.add( i, intent ); return this;}
	
	// methods need to return correct class of this
	public Sign attribute( String name, String value ) {
		append( new Intention( Intention.nameToType( name ), value ));
		return this;
	}

	/* To protect against being interpreted twice on resetting the iterator
	 * after a DNU is returned on co-modification of the iterator by
	 * autoloading repertoires.
	 * 
	 * We may see this pattern in a list of signs following comodification:
	 * 	t t t t f t t t f f t t f f f t t T f f f f f f f f f f f f f f
	 *                                    ^ 
	 * 'cos some new signs will be peppered through out the list. We need
	 * to start again at the sign following "^".
	 * N.B. Don't really want to unload those autoloaded signs as they may well
	 * be needed by the eventually understood utterance.  Autounloading (aging) 
	 * will manage the list if not.
	 * In fact, there should only be one T, the other t's are tidied as 
	 * processing progresses, so there is no need to determine when the...
	 */
	public int interpretation = Signs.noInterpretation;

	// Set during autopoiesis - replaces 'id' attribute 
	private String  concept = "";
	public  String  concept() { return concept; }
	public  Sign    concept( String name ) { concept = name; return this; }
	
	private boolean temporalSet = false;
	private boolean temporal = false;
	public  boolean isTemporal() {
		if (!temporalSet) {
			temporal = Temporal.isConcept( concept );
			temporalSet = true;
		}
		return temporal;
	}

	private boolean spatialSet = false;
	private boolean spatial = false;
	public  boolean isSpatial() {
		if (!spatialSet) {
			spatial = Spatial.isConcept( concept );
			spatialSet = true;
		}
		return spatial;
	}

	private String help = null; // "" is valid output
	public  String help() { return help; }
	public  Sign   help( String str ) { help = str; return this; }
	
	
	/*  The complexity of a sign, used to rank signs in a repertoire.
	 *  "the eagle has landed" comes before "the X has landed", BUT
	 *  "the    X    Y-PHRASE" comes before "the Y-PHRASE" so it is not
	 *  a simple count of tags, phrased hot-spots "hoover-up" tags!
	 *  Phrased hot-spot has a large complexity, and any normal tags
	 *  will bring this complexity down!
	 *  
	 *  Three planes of complexity: bplate hotspots phrased-hotspots
	 *  ==========================
	 *  complexity increases with   1xm bp 1->100.
	 *  complexity increases with 100xn tags 100->10000
	 *  if phrase exists, complexity counts down from 1000000:
	 *  10000 x m bp,   range = 10000 -> 100000
	 *    100 x n tags, range =   100 -> 10000, as before
	 *
	 *  Boilerplate complexity:
	 *  Has been fine tuned to reduce number of clashes when inserting into TreeMap
	 *  Using a random number (less processing?/more random?)  Make it least
	 *  
	 *  Finite:
	 *  |-----------+-----------+------------------|
	 *  | Tags 0-99 |Words 0-99 | Random num 0-99  |
	 *  |-----------+-----------+------------------|
	 *  
	 *  Infinite:
	 *  |----------|   |------------+-----------|------------------+
	 *  | 1000000  | - | Words 0-99 | Tags 0-99 | Random num 0-99  |
	 *  |----------|   |------------+-----------|------------------+
	 *  Range at 1000, random component is 1-100 * 10 - 100 -1000 with 1-99 being
	 *  the count element 
	 */
	static Random rn = new Random();
	static final int      RANGE = 1000; // means full range will be up to 1 billion
	static final int FULL_RANGE = RANGE*RANGE*RANGE;
	static final int  MID_RANGE = RANGE*RANGE;
	static final int  LOW_RANGE = RANGE;
	
	public int complexity() {
		boolean infinite = false;
		int  boilerplate = 0,
		       namedTags = 0,
		             rnd = rn.nextInt( RANGE/10 ); // word count component
		
		for (Tag t : pattern.content()) {
			boilerplate += t.prefix().size();
			if (t.isPhrased()) //attributes().get( phrase ).equals( phrase ))
				infinite = true;
			else if (!t.name().equals( "" ))
				namedTags ++; // count named tags
		}
		// limited to 100bp == 1tag, or phrase + 100 100tags/bp
		return infinite ?
				FULL_RANGE - MID_RANGE*boilerplate - LOW_RANGE*namedTags + rnd
				: MID_RANGE*namedTags + LOW_RANGE*boilerplate + rnd*10;
	}
	
	public Sign content( Tags ta ) { pattern.content( ta ); return this; }
	public Sign content( Tag  t )  { pattern.content.add( t ); return this; }
	
	public String toString( int n, long c ) {
		
		String intents = "";
		for (Intention in : intentions)
			intents += "\n      " + Intention.typeToString( in.type() ) +"+"+ in.value();
		
		return pattern.prefix().toString() + (pattern.name().equals( "" ) ? "" :
			(indent +"<"+ pattern.name() +" n='"+ n +"' complexity='"+ c +"' repertoire='"+ concept() +"'"
			+ intents
			+(null == pattern.content() ? "/>" : ( ">\n"+ indent + indent + pattern.content().toString() + "</"+ pattern.name() +">" ))))
			+ pattern.postfix + "\n";
	}
	
	public Reply mediate( Reply r ) {
		audit.in( "mediate", "\n"+ pattern.toXml() );
		Iterator<Intention> ai = intentions().iterator();
		while (!r.isDone() && ai.hasNext()) {
			Intention in = ai.next();
			r = in.type() == Intention.allop ?
					new Allopoiesis( in.type(), in.value() ).temporalIs( isTemporal()).spatialIs( isSpatial()).mediate( r )
				: in.type() == Intention.append  ||
				  in.type() == Intention.prepend ||
				  in.type() == Intention.create ?
					new Autopoiesis( in.type(), in.value() ).temporalIs( isTemporal()).spatialIs( isSpatial()).mediate( r )
				: // finally, think, do, say...   TODO: why not: in.mediate( r ); ???
					new Intention(   in.type(), in.value() ).temporalIs( isTemporal()).spatialIs( isSpatial()).mediate( r );
		}
		return (Reply) audit.out( r );
	}
	// ---
	public static void complexityTest( Tags t ) {
		Sign container = new Sign();
		container.content( t );
		audit.log( "Complexity of "+ container.toString() +"("+ container.complexity() +")\n");
	}
	public static void main( String argv[]) {
		Sign p = new Sign();
		p.attribute("reply", "hello world");
		p.content( new Tag().prefix( new Strings( "hello" )));
		Reply r = new Reply();
		Intention intent = new Intention( Intention.thenReply, "hello world" );
		r = intent.mediate( r );
		audit.log( "r="+ r.toString());
		
		Tags ts = new Tags();
		ts.add( new Tag( "one small step for man", "" ));
		complexityTest( ts );
		
		ts = new Tags();
		ts.add( new Tag( "this is a", "test" ));
		complexityTest( ts );
		
		ts = new Tags();
		ts.add( new Tag( "this is a test", "x" ).attribute( "phrase", "phrase" ) );
		complexityTest( ts );
		
		ts = new Tags();
		ts.add( new Tag( "this is a", "x" ).attribute( "phrase", "phrase" ));
		complexityTest( ts );
}	}
