package com.yagadi.enguage.util;

// todo: remove use of ArrayList??? or use in throughout??? or LinkedList?
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeSet;

public class Strings extends ArrayList<String> implements Comparable<Strings> {
	
	public static final long serialVersionUID = 0;
	static private Audit audit = new Audit( "Strings" );
	
	public final static int     CSV = 0;
	public final static int   SQCSV = 1;
	public final static int   DQCSV = 2;
	public final static int  SPACED = 3;
	public final static int    PATH = 4;
	public final static int   LINES = 5;
	public final static int  CONCAT = 6;
	public final static int ABSPATH = 7;
	
	public final static String      lineTerm = "\n";
	public final static String           AND = "&&";
	public final static String            OR = "||";
	public final static String    PLUS_ABOUT = "+~";
	public final static String   MINUS_ABOUT = "-~";
	public final static String   PLUS_EQUALS = "+=";
	public final static String  MINUS_EQUALS = "-=";
	public final static String      ELLIPSIS = "...";
	public final static Strings ellipsis = new Strings( ELLIPSIS, '/' );
	
	private String[] tokens = {
			ELLIPSIS,    AND,  OR,
			PLUS_EQUALS, MINUS_EQUALS,
			PLUS_ABOUT,  MINUS_ABOUT };
	
	public Strings() { super(); }
	
	public Strings( Strings orig ) {
		super();
		if (null != orig)
			for (int i=0; i<orig.size(); i++)
				add( orig.get( i ));
	}
	public Strings( String[] sa ) {
		super();
		if (null != sa)
			for (int i=0; i<sa.length; i++)
				add( sa[ i ]);
	}
	
	public Strings( TreeSet<String> sa ) {
		super();
		Iterator<String> i = sa.iterator();
		while (i.hasNext())
			add( i.next());
	}
	public Strings( String buf, char sep ) {
		if (null != buf) {
			int sz = buf.length();
			if (0 < sz) {
				int cp = 0;
				String word = null;
				while( cp<sz ) {
					word="";
					while( cp<sz && (sep != buf.charAt(cp)))
						word += Character.toString( buf.charAt( cp++ )); // *cp++ = *buf++;
					add( new String( word ));
					if ( cp<sz && sep == buf.charAt(cp) ) { // not finished
						cp++;         // avoid separator
						if (cp>=sz) // now finished!
							add( new String( "" )); // add trailing blank string!
	}	}	}	}	}
	private static boolean tokenMatch( String token, String buf, int i, int sz ) {
		int tsz = token.length();
		return (i+tsz <= sz) && token.equals( buf.substring( i, i+tsz ));
	}
	public Strings( String s ) {
		if (s != null && !s.equals( "" )) { // NB this doesn't tie up with parsing in Attributes.c!!!!
			char[] buffer = s.toCharArray();
			int  i = 0, sz = buffer.length;
			while (i<sz) {
				if (Character.isWhitespace( buffer[ i ]))
					i++;
				else {
					StringBuilder word = new StringBuilder( 32 );
					if (Character.isLetter( buffer[ i ])
						|| (   ('_' == buffer[ i ] || '$' == buffer[ i ])
							&& 1+i<sz && Character.isLetter( buffer[ 1+i ])))
					{	//audit.audit("reading AlphaNumeric including embedded apostrophes");
						word.append( buffer[ i++ ]);
						while (i<sz && (
							   Character.isLetter( buffer[ i ])
							|| Character.isDigit(  buffer[ i ])
							||	( '-'  == buffer[ i ])
							||	( '\'' == buffer[ i ])
							||	( '_'  == buffer[ i ])
						))
							word.append( buffer[ i++ ]);
						
					} else if (Character.isDigit( buffer[ i ])
							 ||	(	i+1<sz
								 && Character.isDigit( buffer[ 1+i ])
								 && (	buffer[ i ] =='-'   // -ve numbers
								 	 || buffer[ i ] =='+')) // +ve numbers
							)
					{	//audit.audit("reading NUMBER");
						word.append( buffer[ i++ ]);
						boolean pointDone = false;
						//int     colonsDone = 0;
						while (i<sz
								&& (Character.isDigit( buffer[ i ])
									|| (  !pointDone && buffer[ i ] =='.'
								        && i+1<sz
								        && Character.isDigit( buffer[ 1+i ]))
								        
							//		|| (  colonsDone < 2 && buffer[ i ] ==':'
							//	        && i+2<sz
							//	        && Character.isDigit( buffer[ 1+i ])
							//	        && Character.isDigit( buffer[ 2+i ]))
							  )    )
						{
							if (buffer[ i ] == '.') {
								pointDone = true;
								word.append( buffer[ i++ ]); // point,
								word.append( buffer[ i++ ]); // first decimal
							//} else if (buffer[ i ] == ':') {
							//	colonsDone++;
							//	word.append( buffer[ i++ ]); // one colon,
							//	word.append( buffer[ i++ ]); // two...
							//	word.append( buffer[ i++ ]); // ...decimals.
							} else
								word.append( buffer[ i++ ]);
						}
						
					} else if ('\'' == buffer[ i ] ) {
						// embedded apostrophes: check "def'def", " 'def" or "...def'[ ,.?!]" 
						// quoted string with embedded apostrophes 'no don't'
						//audit.audit("SQ string");
						word.append( buffer[ i++ ]);
						while( i<sz &&
						      !('\'' == buffer[ i ] && // ' followed by WS OR embedded
						        (1+i==sz || Character.isWhitespace( buffer[ 1+i ]))
						     ) ) 
							word.append( buffer[ i++ ]);
						word.append( "'" );
						i++;
						
					} else if ('"' == buffer[ i ]) {
						//audit.audit("DQ string");
						word.append( buffer[ i++ ]);
						while( i<sz && '"' != buffer[ i ])
							word.append( buffer[ i++ ]);
						word.append( '"' ); // always terminate string
						i++;
						
					} else {
						boolean found = false;
						//audit.audit("TOKEN");
						for (int ti=0; ti<tokens.length && !found; ti++)
							if (tokenMatch( tokens[ ti ],  s,  i,  sz )) {
								found=true;
								word.append( tokens[ ti ]);
								i += tokens[ ti ].length();
							}
						if (!found)
							word.append( buffer[ i++ ]);

					}
					if (!word.equals( "" )) {
						//audit.audit(">>>>adding:"+word);
						add( word.toString() );
						word = new StringBuilder( 32 );
					}
		}	}	}
	}
	public static Strings getStrings( String s ) {
		Strings ss = new Strings();
		if (s != null && !s.equals( "" )) { // NB this doesn't tie up with parsing in Attributes.c!!!!
			char[] buffer = s.toCharArray();
			int  i = 0, sz = buffer.length;
			while (i<sz) {
				while (i < sz && Character.isWhitespace( buffer[ i ])) i++;
				StringBuilder word = new StringBuilder( 32 );
				while (i < sz && !Character.isWhitespace( buffer[ i ]))
					word.append( buffer[ i++ ]);
				if (!word.toString().equals( "" )) ss.add( word.toString() );
		}	}
		return ss;
	}
	public String toString( String fore, String mid, String aft ) {
		String as = "";
		int sz = size();
		if (sz > 0) {
			as = fore;
			for (int i=0; i<sz; i++)
				as += ((i == 0 ? "" : mid) + get( i ));
			as += aft;
		}
		return as;
	}
	public String toString( int n ) {
		return
			( n ==  SPACED ) ? toString(   "",      " ",   "" ) :
			( n ==  CONCAT ) ? toString(   "",       "",   "" ) :
			( n ==   DQCSV ) ? toString( "\"", "\", \"", "\"" ) :
			( n ==   SQCSV ) ? toString(  "'",   "', '",  "'" ) :
			( n ==     CSV ) ? toString(   "",      ",",   "" ) :
			( n ==    PATH ) ? toString(   "",      "/",   "" ) :
			( n ==   LINES ) ? toString(   "",     "\n",   "" ) :
			( n == ABSPATH ) ? toString(   "/",     "/",   "" ) :
			"Strings.toString( "+ toString( CSV ) +", n="+ n +"? )";
	}
	public String toString() { return toString( SPACED ); }
	public String toString( Strings seps ) {
		if (size() == 0)
			return "";
		else if (null == seps)
			return toString( SPACED );
		else if (seps.size() == 1)
			return toString( "", seps.get( 0 ), "" );
		else if (seps.size() == 2) { // oxford comma: ", ", ", and "
			String rc = "";
			ListIterator<String> li = listIterator();
			if (li.hasNext()) {
				rc = (String) li.next();
				String first = seps.get( 0 ),
				       last = seps.get( 1 );
				while (li.hasNext()) {
					String tmp = (String) li.next();
					rc += (li.hasNext() ? first : last) + tmp;
			}  }
			return rc;
		} else if (seps.size() == 4) {
			Strings tmp = new Strings();
			tmp.add( seps.get( 1 ));
			tmp.add( seps.get( 2 ));
			return seps.get( 0 ) + toString( tmp ) + seps.get( 3 );
		} else 
			return toString( seps.get( 0 ), seps.get( 1 ), seps.get( 2 ));
	} // don't use traceOutStrings here -- it calls Strings.toString()!
	/* --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- 
	static private String[] fromString( String buf ) {
		Strings a = new Strings( buf );
		return Strings.fromArrayList( a );
	}*/
	static public String getString( ListIterator<String> si, int n ) {
		Strings sa = new Strings();
		for (int i=0; i<n; i++)
			if (si.hasNext())
				sa.add( si.next());
		return sa.toString( Strings.SPACED );
	}
	public Strings filter() {
		// remove any [ superfluous ] stuff
		Strings filtered = new Strings();
		ListIterator<String> li = listIterator();
		while (li.hasNext()) {
			String item=li.next();
			if (item.equals( "[" )) // skip to closing ]
				while (li.hasNext() && !item.equals( "]" ))
					item=li.next();
			else
				filtered.add( item );
		}
		return filtered;
	}
	public Strings removeAll( String val ) {
		Iterator<String> ai = iterator();
		while (ai.hasNext()) {
			if (ai.next().equals( val ))
				ai.remove();
		}
		return this;
	}
	// EITHER:
	// (a=[ "One Two Three", "Ay Bee Cee", "Alpha Beta" ], val= "Bee") => "Ay Bee Cee";
	//static public String getContext( String[] a, String val ) {
	//	return "incomplete";
	//}
	// OR: (outer=[ "a", "strong", "beer" ], inner=[ "strong", "beer" ]) => true
/*	static public boolean xcontainsStrings( String[] outer, String[] inner ) {
		if (outer.length == 0 && 0 == inner.length)
			return true;
		else if (outer.length >= inner.length)
			for (int o=0; o<=outer.length-inner.length; o++)
				for (int i=0; i<inner.length; i++)
					if (outer[ o + i ].equals( inner[ i ])) return true;
		return false;
	} // */
	public boolean containsMatched( Strings inner ) {
		boolean rc = false;
		if (size() == 0 && 0 == inner.size())
			return true;
		else if (size() >= inner.size())
			// this loop goes thru outer in a chunk size of inner
			for (int o=0; rc == false && o<=size()-inner.size(); o++) {
				// see if the inner chunk matches from posn o
				rc = true; // lets assume it does
				for (int i=0; rc == true && i<inner.size(); i++)
					if (!get( o + i ).equals( inner.get( i ))) // if one doesn't match
						rc = false;
			}
		return rc;
	}
	// ...OR: -------------------------------------------
	// a=[ "One Two Three", "Aye Bee Cee", "Alpha Beta" ], val= "Bee" => b = [ "One Two Three", "Alpha Beta" ];
	public Strings removeAllMatched( String val ) {
		Strings b = new Strings();
		Strings valItems = new Strings( val );
		for (int ai=0; ai<size(); ai++) 
			if (!new Strings( get( ai )).containsMatched( valItems ))
				b.add( get( ai ));
		return b;
	}
	// ---------------------------------------------
	public Strings removeFirst( String val ) {
		Iterator<String> si = iterator();
		while (si.hasNext())
			if (si.next().equals( val )) {
				si.remove();
				break;
			}
		return this;
	}
	public String remove( int i ) {
		String str = "";
		if (i >= 0 && i<size()) {
			str = get( i );
			super.remove( i );
		} else
			audit.ERROR( "trying to remove "+ i +(i%10==1&&i!=11?"st":i%10==2&&i!=12?"nd":i%10==3&&i!=13?"rd":"th")+ " element of list of "+ size() +" items" );
		return str;
	}
	public Strings remove( int i, int n ) {
		Strings strs = new Strings();
		if (i >= 0 && (i + n) <= size())
			for (int j=0; j<n; j++ ) {
				strs.add( get( i ));
				super.remove( i );
			}
		else if (n > 0)
			audit.ERROR( "trying to remove "+ n +" elements at the "+ i +(i%10==1&&i!=11?"st":i%10==2&&i!=12?"nd":i%10==3&&i!=13?"rd":"th")+ " position in list of "+ size() +" items" );
		return strs;
	}
	public Strings contract( String item ) {
		int sz=size()-1;
		for( int i=1; i<sz; i++ )
			if (get( i ).equals( item )) {
				set( i-1, get( i-1 )+ item +remove( i+1 ) );
				remove( i );
				sz -= 2;
			}
		return this;
	}
	public Strings replace( int i, String s ) {
		remove( i );
		add( i, s );
		return this;
	}
	public Strings append( Strings sa ) {
		addAll( sa );
		return this;
	}
	public Strings append( String s ) {
		add( s );
		return this;
	}
	public Strings prepend( String str ) {
		add( 0, str );
		return this;
	}
	public Strings copyFrom( int n ) {
		Strings b = new Strings();
		for (int i=n, sz = size(); i<sz; i++)
			b.add( get( i ));
		return b;
	}
	public Strings copyAfter( int n ) {
		Strings b = new Strings();
		for (int i=n+1, sz = size(); i<sz; i++)
			b.add( get( i ));
		return b;
	}
	public Strings copyFromUntil( int n, String until ) {
		Strings b = new Strings();
		for (int i=n, sz = size(); i<sz; i++) {
			String item = get( i );
			if (item.equals( until ))
				break;
			else
				b.add( item );
		}
		return b;
	}
	static public Strings fromNonWS( String buf ) {
		Strings a = new Strings();
		if (buf != null) {
			StringBuffer word = null;
			for (int i=0, sz=buf.length(); i<sz; i++ ) {
				word = new StringBuffer();
				while( i<sz &&  Character.isWhitespace( buf.charAt( i ))) i++;
				while( i<sz && !Character.isWhitespace( buf.charAt( i ))) { word.append( buf.charAt( i )); i++; }
				
				if (null != word)
					a.add( word.toString());
		}	}
		return a;
	}
	public Strings reverse() {
		Strings b = new Strings();
		for (int sz=size(), i=sz-1; i>=0; i--)
			b.add( get( i ));
		return b;
	}
	//      [ "hello", "martin", "!" ].replace([ "martin" ], [ "to", "you" ]) => [ "hello", "to", "you", "!" ]
	// err: [ "hello", "martin", "!" ].replace([ "martin" ], [ "to", "martin" ]) => [ "hello", "martin", "!" ]
	public Strings replace( Strings b, Strings c ) {
		//audit.traceIn("replace", b.toString() +" with "+ c.toString() +" in "+ toString());
		int len = size(), blen = b.size(), clen = c.size();
		for (int i=0; i <= len - blen; i++) {
			boolean found = true;
			int j=0;
			for (j=0; j<blen && found; j++)
				if (!get( i+j ).equalsIgnoreCase( b.get( j ))) found=false;
			if (found) {
				for (j=0; j<blen; j++) remove( i );
				for (j=0; j<clen; j++) add( i+j, c.get( j ));
				i += clen;    // advance counter over replaced Strings
				len = size(); // ...reset len since we've messed with a	
		}	}
		//audit.traceOut( toString());
		return this;
	}
	public Strings replace( Strings a, String b ) { return replace( a, new Strings( b ));}
	public boolean contains( Strings a ) {
		int len = size(), alen = a.size();
		for (int i=0; i <= len - alen; i++) {
			boolean found = true;
			int j=0;
			for (j=0; j<alen && found; j++)
				if (!get( i+j ).equalsIgnoreCase( a.get( j )))
					found=false;
			if (found) return true;
		}
		return false;
	}
	
	public static void removeN( ListIterator<String> si, int n ) {
		//audit.debug( "Removing "+ n );
		for (int i=0; i<n; i++) si.remove();
	}
	public static void previousN( ListIterator<String> si, int n ) {
		//audit.debug( "Previous "+ n );
		for (int i=0; i<n; i++) si.previous();
	}
	// count the number of matching strings - due for Strings class!!!
	public int matches( ListIterator<String> li ) {
		//audit.in( "matches", "'"+ toString( Strings.SPACED ) +"', "+ li.toString());
		int n = 0, m=0;
		ListIterator<String> pi = listIterator();
		while (li.hasNext() && pi.hasNext()) {
			m++;
			if (li.next().equals( pi.next() ))
				n++;
			else
				break;
		}
		//put li back
		previousN( li, m );
		//return audit.out( pi.hasNext() ? 0 : n );
		return pi.hasNext() ? 0 : n;
	}
	
	// deals with matched and unmatched values:
	// [ "a", "$matched", ".",  "b" ] => [ "a", "martin", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
	// [ "a",  "MATCHED", ".",  "b" ] => [ "a", "martin", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
	// [ "a", "$unmatch", ".",  "b" ] => [ "a", "_USER", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
	// [ "we are holding hands", "."  ] => [ "we", "are", "holding", "hands", "." ] -- jik - just in case!
	// matches are from tags, *ap contains mixed case - any UPPERCASE items should match matches OR envvars.
	// [ 'some', 'bread', '+', 'fish'n'chips', '+', 'some', 'milk' ], "+"
	//                                => [  'some bread', 'fish and chips', 'some milk' ]
	private Strings normalise( String ipSep, String opSep ) {
		//audit.traceIn( "normalise", "ipSep='"+ ipSep +"' opSep='"+ opSep +"'");
		// remember, if sep='+', 'fish', '+', 'chips' => 'fish+chips' (i.e. NOT 'fish + chips')
		// here: some coffee + fish + chips => some coffee + fish and chips
		Strings values = new Strings();
		if (size() > 0) {
			int i = 0;
			String value = get( 0 ); //
			//audit.audit(":gotVal:"+ value +":");
			String localSep = opSep; // ""; // only use op sep on appending subsequent strings
			while (++i < size()) {
				String tmp = get( i );
				//audit.audit(":gotTmp:"+ tmp +":");
				if (tmp.equals( ipSep )) {
					//audit.audit("normalise():adding:"+ value +":");
					values.add( value );
					value = "";
					localSep = "";
				} else {
					value += ( localSep + tmp );
					localSep = opSep;
					//audit.audit(":valNow:"+ value +":");
			}	}
			//audit.audit("normalise():adding:"+ value +":");
			values.add( value );
		}
		//return audit.traceOut( values );
		return values;
	}
	/*
	 * normalise with a parameter uses that param as a user defined separator, rather than whitespace
	 * normalise([ "one", "two", "+", "three four" ], "+") => [ "one two", "three four" ]
	 */
	public Strings normalise( String sep ) {
		return normalise( sep, " " );
	}
	// normalise([ "one", "two three" ]) => [ "one", "two", "three" ]
	public Strings normalise() {
		Strings a = new Strings();
		for (String s1 : this )
			for ( String s2: new Strings( s1 ))
				a.add( s2 );
		return a;
	}
	
	// TODO: expand input, and apply each thought...
	// I need to go to the gym and the jewellers =>
	// (I need to go to the gym and I need to go to the jewellers =>)
	// I need to go to the gym. I need to go to the jewellers.
	/* [ [ "to", "go", "to", "the", "gym" ], [ "the", "jewellers" ] ] 
	 * => [ [ "to", "go", "to", "the", "gym" ], [ "to", "go", "to", "the", "jewellers" ] ]
	 */
	// [ "THIS", "is", "Martin" ] => [ "THIS", "is", "martin" ]
	public Strings decap() {
		int i = -1;
		//remove all capitalisation... we can re-capitalise on output.
		while (size() > ++i) {
			String tmp = get( i );
			if (isCapitalised( tmp ))
				set( i, Character.toLowerCase( tmp.charAt( 0 )) + tmp.substring( 1 ));
		}
		return this;
	}
	private static boolean isCapitalised( String str ) {
		if (null == str) return false;
		int len = str.length();
		if (Character.isUpperCase( str.charAt( 0 )) && len > 1) {
			int i = 0;
			while (len > ++i && Character.isLowerCase( str.charAt( i )))
				;
			return str.length() == i; // capitalised if we're at the end of the string
		}
		return false;
	}
	public static boolean isUpperCase( String a ) {
		for (int i=0; i<a.length(); i++)
			if (!Character.isUpperCase( a.charAt( i )) )
				return false;
		return true;
	}
	public static boolean isUpperCaseWithHyphens( String a ) {
		for (int i=0; i<a.length(); i++)
			if (!Character.isUpperCase( a.charAt( i )) && a.charAt( i ) != '-' )
				return false;
		return true;
	}
	public Strings trimAll( char ch ) {
		int i=0;
		for( String s : this )
			set( i++, trim( s, ch ));
		return this;
	}
	static public String trim( String a, char ch ) { return triml( a, a.length(), ch ); }
	static public String triml( String a, int asz, char ch ) {
		// (a="\"hello\"", ch='"') => "hello"; ( "ohio", 'o' ) => "hi"
		char ch0 = a.charAt( 0 );
		if (asz == 2 && ch0 == ch && a.charAt( 1 ) == ch)
			return "";
		else if (asz > 2 && ch0 == ch && a.charAt( asz-1 ) == ch)
			return a.substring( 1, asz-1 );
		else
			return a;
	}
	static public String stripQuotes( String s ) {
		int sz = s.length();
		if (sz>1) {
			char ch = s.charAt( 0 );
			     if (ch == '\'') s = Strings.triml( s, sz, '\'' );
			else if (ch ==  '"') s = Strings.triml( s, sz,  '"' );
		}
		return s; 
	}
	
	// ---------------------------------------------------------
	// ---------------------------------------------------------
	/* 
	 * combine and divide --
	 * if a single separator, don't need to store that separator, combine just adds it
	 * if a combination of separators, we need to remember which one it is so it can be added!
	 * 
	 */
	// backwards compatibility -- include terminators
	public ArrayList<Strings> divide( Strings separators ) { return divide( separators, true ); }
	public ArrayList<Strings> divide( Strings terminators, boolean inclusive ) {
		// [ "o", "t", ".", "t", "?", "f", "f" ]( ".?!" ) => [["o", "t", "."], ["t", "?"], ["f", "f"]]
		ArrayList<Strings> divisions = new ArrayList<Strings>();
		Strings division = new Strings();
		for (String s : this) {
			if (inclusive || !terminators.contains( s )) division.add( s );
			if (terminators.contains( s )) {
				divisions.add( division );
				division = new Strings();
		}	}
		divisions.add( division );
		return divisions;
	}
	static Strings combine( ArrayList<Strings> as ) {
		// [["o", "t". "."], ["t", "?"], ["f", "f"]] => [ "o", "t", ".", "t", "?", "f", "f" ]
		Strings sa = new Strings();
		for (Strings tmp : as)
			sa.addAll( tmp );
		return sa;
	}
	// ---------------------------------------------------------
	public ArrayList<Strings> divide( String sep ) {
		// [ "o", "t", "&", "t", "?", "&", "f", "f" ]( "&" ) => [["o", "t", "."], ["t", "?"], ["f", "f"]]
		ArrayList<Strings> divisions = new ArrayList<Strings>();
		Strings division = new Strings();
		for (String s : this) {
			if (sep.equals( s )) {
				divisions.add( division );
				division = new Strings();
			} else {
				division.add( s );
		}	}
		divisions.add( division );
		return divisions;
	}
	static Strings combine( ArrayList<Strings> as, String sep ) {
		// [["o", "t"], ["t", "?"], ["f", "f"]] => [ "o", "t", "&", "t", "?", "&", "f", "f" ]
		Strings sa = new Strings();
		boolean first = true;
		for (Strings tmp : as) {
			if (first)
				first = false;
			else
				sa.add( sep );
			sa.addAll( tmp );
		}
		return sa;
	}
	
	public int compareTo( Strings sa ) {
		/* This compareTo() will put the longer strings first so:
		 * "user", "does", "not"  matches before  "user", "does"
		 */
		int rc = 0;
		Iterator<String> i = iterator(),
		               sai = sa.iterator();
		while (rc==0 && i.hasNext() && sai.hasNext())
			rc = sai.next().compareTo( i.next() );
		
		if (rc==0 && (i.hasNext() || sai.hasNext()))
			rc = i.hasNext() ? -1 : 1 ;
			
		return rc;
	}

	public boolean equalsIgnoreCase( Strings sa ) {
		Iterator<String> i = iterator(),
		               sai = sa.iterator();
		while (i.hasNext() && sai.hasNext())
			if (!sai.next().equalsIgnoreCase( i.next() ))
				return false;
		return !i.hasNext() && !sai.hasNext();
	}
	public boolean equals( Strings sa ) {
		Iterator<String> i = iterator(),
		               sai = sa.iterator();
		while (i.hasNext() && sai.hasNext())
			if (!sai.next().equals( i.next() ))
				return false;
		return !i.hasNext() && !sai.hasNext();
	}
	public boolean beginsIgnoreCase( Strings sa ) {
		Iterator<String> i = iterator(),
		               sai = sa.iterator();
		while (i.hasNext() && sai.hasNext())
			if (!sai.next().equalsIgnoreCase( i.next() ))
				return false;
		return !sai.hasNext();
	}

	public static boolean doString( String val, ListIterator<String> si ) {
		if (si.hasNext()) {
			if (si.next().equals( val ))
				return true;
			si.previous();
		}
		return false;
	}

	public static ListIterator<String> resetList( Strings sa, int start, ListIterator<String> si) {
		si = sa.listIterator();
		while (si.hasNext() && si.nextIndex() != start) si.next();
		return si;
	}

	// ---------------------------------------------------------
	
	public static void main( String args[]) {
		Audit.allOn(); //main()
		audit.log( "hello, world" );
		
		Strings a = new Strings( "hello there" ),
				b = new Strings( "hello world" ),
		        c = new Strings( "hello there martin" );
		
		audit.log( "comparing "+ a +" to "+ b +" = "+ (a.compareTo( b ) > 0 ? "pass" : "fail" ));
		audit.log( "comparing "+ a +" to "+ c +" = "+ (a.compareTo( c ) > 0 ? "pass" : "fail" ));
		
		b = new Strings( c );
		b.remove( 2 );
		audit.log( "remove from a copy: b is "+ b.toString( Strings.SPACED ) +", c is "+ c.toString( Strings.SPACED ) );
		
		
		
		audit.log( "a: "+ new Strings( "failure won't 'do' 'don't'" ));
		audit.log( "b: "+ new Strings( "..........." ));
		audit.log( "c: "+ new Strings( "+2.0" ));
		audit.log( "d: "+ new Strings( "quantity+=2.0" ));
		
		a = new Strings("hello failure");
		b = new Strings( "failure" );
		c = new Strings( "world" );
		audit.log( "e: ["+ a.replace( b, c ).toString( "'", "', '", "'" ) +"]" );
		String tmp = "+=6";
		audit.log( "tmp: "+ tmp.substring( 0, 1 ) + tmp.substring( 2 ));
	
		audit.log("tma:"+(tokenMatch( ELLIPSIS, ELLIPSIS, 0, ELLIPSIS.length() )?"true":"false")+"=>true");
		audit.log("tma:"+(tokenMatch( ELLIPSIS, ELLIPSIS, 1, ELLIPSIS.length() )?"true":"false")+"=>false");
		audit.log("tma:"+(tokenMatch( ELLIPSIS,     "..", 0,     "..".length() )?"true":"false")+"=>false");
		
		a = new Strings( "this is a test sentence. And half a" );
		ArrayList<Strings> as = a.divide( Shell.terminators() );
		// as should be of length 2...
		b = as.remove( 0 );
		audit.log( "b is '"+ b.toString() +"'. as is len "+ as.size() );
		a = Strings.combine( as ); // needs blank last item to add terminating "."
		audit.log( "a is '"+ a.toString() +"'. a is len "+ a.size() );
		a.addAll( b );
		audit.log( "a is now '"+ a.toString() +"'." );
		
		audit.log( "begins:"+ ( new Strings("to be or not").beginsIgnoreCase(new Strings("to be"))? "pass":"fail" ));
		audit.log( "begins:"+ ( new Strings("to be or not").beginsIgnoreCase(new Strings("to be"))? "pass":"fail" ));
		audit.log( "begins:"+ ( new Strings("to be").beginsIgnoreCase(new Strings("to be or"))? "fail":"pass" ));
		
		a = new Strings( "17:45:30:90" );
		audit.log( "the time is "+ a.toString( SPACED ));

		a = new Strings( "this is a test" );
		Strings seps = new Strings( ", / and ", '/' );
		audit.log( a.toString( seps ) );
// */
		/* /
		String s = "this test should pass";
		Strings sa1 = new Strings( s, ' ' );
		//Strings sa2 = new Strings( s, " " );
		//String[] sa3 = Strings.fromLines( "this\ntest\nshould\npass" );
		//audit.audit( "equals test "+ (sa1.equals( sa2 ) ? "passes" : "fails" ));
		audit.audit( "===> ["+ sa1.toString( "'", "', '", "'" ) +"] <===" );
		//audit.audit( "===> ["+ sa2.toString( Strings.SQCSV ) +"] <===" );
	
		
		//static public String[] removeAt( String[] a, int n ) ;
		//static public String[] removeAll( String[] a, String val ) ;
		// EITHER:
		//String[] a = new String[] {"One Two Three", "this test passes", "Alpha Beta" };
		//String  val= "passes";
		//audit.audit( "getContext test: "+ getContext( a, val ));
		
		Strings outer = new Strings( "a strong beer" );
		Strings inner = new Strings( "strong beer" );
		audit.audit( "containsStrings test "+ (outer.containsMatched( inner ) ? "passes" : "fails" ));
		// ...OR: -------------------------------------------
		// a=[ "One Two Three", "Aye Bee Cee", "Alpha Beta" ], val= "Bee" => b = [ "One Two Three", "Alpha Beta" ];
		//static public String[] removeAllMatched( String[] a, String val ) ;
		// ---------------------------------------------
		//static public String[] removeFirst( String[] a, String val ) ;
		//audit.audit( toString( removeFirst( Strings.fromString( "this test passes" ), "test" ), SPACED ));
		//static public String[] append( String[] a, String str ) ;
		//audit.audit( toString( append( Strings.fromString( "this test " ), "passes" ), SPACED ));
		//static public String[] append( String[] a, String sa[] ) ;
		//audit.audit( toString( append( fromString( "this test " ), fromString( "passes" )), SPACED ));
		//static public String[] prepend( String[] a, String str ) ;
		//audit.audit( toString( prepend( fromString( "test passes" ), "this" ), Strings.SPACED ));
		//static public String[] copyAfter( String[] a, int n ) ;
		//audit.audit( toString( copyAfter( fromString( "error this test passes" ), 0 ), SPACED ));
		//static public String[] copyFromUntil( String[] a, int n, String until ) ;
		//Strings xxx = new Strings( "error this test passes error" );
		//audit.audit( toString( xxx.copyFromUntil( 1, "passes" ), SPACED ));
		//static public String[] fromNonWS( String buf ) ;
	/*	audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public String[] insertAt( String[] a, int pos, String str ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public String[] reverse( String[] a ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		// replace( [ "hello", "martin", "!" ], [ "martin" ], [ "to", "you" ]) => [ "hello", "to", "you", "!" ]
		//static public String[] replace( String[] a, String[] b, String[] c ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public int indexOf( String[] a, String s ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public boolean contain( String[] a, String s ) ; return -1 != indexOf( a, s ); }
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		
		// deals with matched and unmatched values:
		// [ "a", "$matched", ".",  "b" ] => [ "a", "martin", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
		// [ "a",  "MATCHED", ".",  "b" ] => [ "a", "martin", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
		// [ "a", "$unmatch", ".",  "b" ] => [ "a", "_USER", "." ] += [ "b", "." ] -- add period like Tag.c::newTagFromDesc...()
		// [ "we are holding hands", "."  ] => [ "we", "are", "holding", "hands", "." ] -- jik - just in case!
		// matches are from tags, *ap contains mixed case - any UPPERCASE items should match matches OR envvars.
		//static public String[][] split( String[] a, String[] terminators ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));

		// [ 'some', 'bread', '+', 'fish'n'chips', '+', 'some', 'milk' ], "+"  => [  'some bread', 'fish and chips', 'some milk' ]
		//static public String[] rejig( String[] a, String ipSep, String opSep ) ;
		//static public String[] rejig( String[] a, String sep ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		// todo: remove rejig, above? Or, combine with expand() and normalise()/
		// NO: Need Stringses to Strings & vv [ "some", "beer", "+", "some crisps" ] => "some beer", "some crisps" ]
		// [ "some beer", "some crisps" ] => [ "some", "beer", "+", "some", "crisps" ]
		// todo: expand input, and apply each thought...
		// I need to go to the gym and the jewellers =>
		// (I need to go to the gym and I need to go to the jewellers =>)
		// I need to go to the gym. I need to go to the jewellers.
		//static public String[][] expand( String[][] a ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));

		// [ "one", "two three" ] => [ "one", "two", "three" ]
		// todo: a bit like re-jig aove???
		//static public String[] normalise( String[] sa ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public boolean isUpperCase( String a ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		//static public boolean isUpperCaseWithHyphens( String a ) ;
		audit.audit( toString( ( Strings.fromString( "this test " ), "passes" ), Strings.SPACED ));
		 *
		 */
		// [ "THIS", "is Martin" ] => [ "THIS", "is", "martin" ]
		//audit.audit( Strings.toString( decap( Strings.fromString( "THIS is Martin" )), Strings.DQCSV ) +" should equal [ \"THIS\", \"is\", \"martin\" ]" );
		//audit.audit( trim( "\"hello\"", '"' ) +" there == "+ trim( "ohio", 'o' ) +" there! Ok?" );
		
		//audit.audit( Strings.toString( Strings.fromString( "failure won't 'do' 'don't'" ), Strings.DQCSV ));
		//audit.audit( Strings.toString( Strings.insertAt( Strings.fromString( "is the greatest" ), -1, "martin" ), Strings.SPACED ));
		//audit.audit( "" );
}	}
