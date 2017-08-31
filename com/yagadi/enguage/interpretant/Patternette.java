package com.yagadi.enguage.interpretant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Locale;

import com.yagadi.enguage.object.Attribute;
import com.yagadi.enguage.object.Attributes;
import com.yagadi.enguage.util.Audit;
import com.yagadi.enguage.util.Fs;
import com.yagadi.enguage.util.Indent;
import com.yagadi.enguage.util.Number;
import com.yagadi.enguage.util.Strings;
import com.yagadi.enguage.vehicle.Language;
import com.yagadi.enguage.vehicle.Plural;

public class Patternette {
	private static Audit audit = new Audit( "Patternette" );
	
	public static final int NULL   = 0;
	public static final int ATOMIC = 1;
	public static final int START  = 2;
	public static final int END    = 3;
	
	public static final String emptyPrefix = "";

	private int  type = NULL;
	private int  type() { return type; }
	private void type( int t ) { if (t>=NULL && t<=END) type = t; }

	public static final String quoted = "quoted";
	public static final String quotedPrefix = quoted.toUpperCase( Locale.getDefault() ) + "-";
	public static final String plural = Plural.NAME; // "plural";
	public static final String pluralPrefix = plural.toUpperCase( Locale.getDefault()) + "-";
	public static final String singular = "singular";
	public static final String singularPrefix = singular.toUpperCase( Locale.getDefault() ) + "-";
	public static final String abstr  = "abstract";
	
	
	private Strings prefix = new Strings();
	public  Strings prefix() { return prefix; }
	public  Patternette     prefix( Strings s ) { prefix = s; return this; }
	public  Patternette     prefix( String str ) { prefix.append( str ); return this; }

	public Strings postfixAsStrings;
	public Strings postfixAsStrings() { return new Strings( postfix ); }
	public String  postfix = "";
	public String  postfix(  ) { return postfix; }
	public Patternette     postfix( String str ) { postfix = str; return this; }

	private String name = "";
	public  String name() { return name; }
	public  Patternette    name( String nm ) { if (null != nm) name = nm; return this; }

	private boolean isNumeric = false;
	public  boolean isNumeric() { return isNumeric; }
	public  Patternette     numericIs( boolean nm ) { isNumeric = nm; return this; }

	private boolean isQuoted = false;
	public  boolean quoted() { return isQuoted;}
	
	private boolean isPlural = false;
	public  boolean pluraled() { return isPlural; }
	
	private boolean isPhrased = false;
	public  boolean isPhrased() { return isPhrased; }
	
	private Attributes attrs = new Attributes();
	public  Attributes attributes() { return attrs; }
	public  Patternette        attributes( Attribute a ) {
		if (a.name().equals( Pattern.phrase ))
			isPhrased = true;
		else if (a.name().equals( plural ))
			isPlural = true;
		else if (a.name().equals( quoted ))
			isQuoted = true;
		else if (a.name().equals( Pattern.numeric ))
			numericIs( true );
		else
			attrs.add( a );
		return this;
	}
	public  Patternette        attributes( Attributes as ) {
		for( Attribute a : as)
			attributes( a ); // add each individually
		// Propagate num chars read on creation...
		attrs.nchars( as.nchars());
		return this;
	}
	public  String     attribute( String name ) { return attrs.get( name ); }
	public  Patternette        attribute( String name, String value ) {attributes( new Attribute( name, value )); return this; }
	// ordering of attributes relevant to Autopoiesis
	public  Patternette        append( String name, String value ) { attributes( new Attribute( name, value )); return this; }
	public  Patternette        prepend( String name, String value ) { return add( 0, name, value );}  // 0 == add at 0th index!
	public  Patternette        add( int posn, String name, String value ) {
		if (name.equals( Pattern.phrase ))
			isPhrased = true;
		else
			attrs.add( posn, new Attribute( name, value ));
		return this;
	}
	public  Patternette        remove( int nth ) { attrs.remove( nth ); return this; }
	public  Patternette        remove( String name ) { attrs.remove( name ); return this; }
	public  Patternette        replace( String name, String value ) {
		attrs.remove( name );
		attrs.add( new Attribute( name, value ));
		return this;
	}
	
	public Attribute matchedAttr( String val ) {
		return new Attribute(
				name,
				Attribute.expandValues( // prevents X="x='val'"
					name.equals("unit") ? Plural.singular( val ) : val
				).toString( Strings.SPACED ) );
	}

	
	public boolean isEmpty() { return name.equals("") && prefix().size() == 0; }

	public boolean invalid( ListIterator<String> ui  ) {
		boolean rc = false;
		if (ui.hasNext()) {
			String candidate = ui.next();
			rc = (  quoted() && !Language.isQuoted( candidate ))
			  || (pluraled() && !Plural.isPlural(   candidate ))
			  // Useful for preventing class "class" being created...
			  //|| (attribute( Tag.abstr ).equals( Tag.abstr ) && // Archaic? Refactor anyway!
				//		candidate.equalsIgnoreCase( name()    )   )
						;
			if (ui.hasPrevious()) ui.previous();
		}
		return rc;
	}
	
	// --
	public Patternette update( Attributes as ) {
		/*
		 * this contains some item specific code :(
		 */
		audit.in( "update", as.toString() );
		for (Attribute a : as) {
			String value = a.value(),
					name = a.name();
			if (name.equals( "quantity" )) {
				/*
				 * should getNumber() and combine number from value.
				 */
				Strings vs = new Strings( value );
				if (vs.size() == 2) {
					/*
					 * combine magnitude - replace if both absolute, add if relative etc.
					 * Should be in Number? Should deal with "1 more" + "2 more" = "3 more"
					 */
					String firstVal = vs.get( 0 ), secondVal = vs.get( 1 );
					if (secondVal.equals( Number.MORE ) || secondVal.equals( Number.FEWER )) {
						int oldInt = 0, newInt = 0;
						try {
							oldInt = Integer.valueOf( attribute( name ));
						} catch (Exception e) {} // fail silently, oldInt = 0
						//audit.debug( "oldInt is "+ oldInt );
						try {
							newInt = Integer.valueOf( firstVal );
							//audit.debug( "newInt is "+ newInt );
							/*
							 * What the ...?
							 */
							value = Integer.toString( oldInt + (secondVal.equals( Number.MORE ) ? newInt : -newInt));
							//audit.debug( "value  is "+ value );
							
						} catch (Exception e) {} // fail silently, newInt = 0;
				}	}
			//} else if (name.equals( "unit" )) {
			}
			replace( a.name(), value );
		}
		audit.out();
		return this;
	}
//	public int remove( Patternette pattern ) {
//		//audit.traceIn( "remove", pattern.toString());
//		int rc = 0;
//		Iterator<Patternette> i = content().iterator();
//		while (i.hasNext()) {
//			Patternette t = i.next();
//			audit.debug( "checking against: "+ t.toString() );
//			if (t.equals( pattern )) {
//				i.remove();
//				rc++;
//		}	}
//		//audit.traceut( rc );
//		return rc;
//	}
//	public int removeMatches( Patternette pattern ) {
//		int rc = 0;
//		Iterator<Patternette> i = content().iterator();
//		while (i.hasNext()) {
//			Patternette t = i.next();
//			if (t.matches( pattern )) {
//				i.remove();
//				rc++;
//		}	}
//		return rc;
//	}
	public boolean equals( Patternette pattern ) {
		//if (audit.tracing) audit.in( "equals", "this="+ toXml() +", with="+ pattern.toXml());
		boolean rc;
		if (attributes().size() < pattern.attributes().size()) { // not exact!!!
			//audit.audit( " T:Size issue:"+ attributes().size() +":with:"+ pattern.attributes().size() +":");
			rc = false;
//		} else if ((content().size()==0 && pattern.content().size()!=0) 
	//			|| (content().size()!=0 && pattern.content().size()==0)) {
		//	//audit.audit(" T:one content empty -> false");
			//rc =  false;
		} else {
			//audit.audit( " T:Checking also attrs:"+ attributes().toString() +":with:"+ pattern.attributes().toString() +":" );
			//audit.audit( " T:Checking also prefx:"+ prefix() +":with:"+ pattern.prefix() +":" );
			rc =   Plural.singular(  prefix.toString()).equals( Plural.singular( pattern.prefix().toString()))
				&& Plural.singular( postfix()).equals( Plural.singular( pattern.postfix()))
				&& attributes().matches( pattern.attributes()) // not exact!!!
				//&& content().equals( pattern.content())
						;
			//audit.audit( " T:full check returns: "+ rc );
		}
		//if (audit.tracing) audit.out( rc );
		return rc;
	}
	/* <fred attr1="a" attr2="b">fred<content/> bill<content/></fred>.(
	//		<fred attr2="b"/> -> true
	// )
	 * 
	 */
	public boolean matches( Patternette pattern ) {
		//if (audit.tracing) audit.in( "matches", "this="+ toXml() +", pattern="+ pattern.toXml() );
		boolean rc;
		if (attributes().size() < pattern.attributes().size()) {
			//audit.debug( "Too many attrs -> false" );
			rc = false;
//		} else if ((content().size()==0 && pattern.content().size()!=0) ){
	//		//	|| (content().size()!=0 && pattern.content().size()==0)) {
	//		//audit.audit("one content empty -> false");
		//	rc =  false;
		} else {
			//audit.debug( "Checking also attrs:"+ attributes().toString() +":with:"+ pattern.attributes().toString() );
			rc =   Plural.singular( prefix().toString()).contains( Plural.singular( pattern.prefix().toString()))
				&& postfix().equals( pattern.postfix())
				&& attributes().matches( pattern.attributes())
				//&&    content().matches( pattern.content())
				;
			//audit.debug("full check returns: "+ rc );
		}
		//if (audit.tracing) audit.out( rc );
		return rc;
	}
	public boolean matchesContent( Patternette pattern ) {
		//audit.traceIn( "matches", "this="+ toString() +", pattern="+ pattern.toString() );
		boolean rc;
//		if (   (content().size()==0 && pattern.content().size()!=0) 
//			|| (content().size()!=0 && pattern.content().size()==0)) {
//			//audit.audit("one content empty -> false");
//			rc =  false;
//		} else {
			//audit.debug( "Checking also attrs:"+ attributes().toString() +":with:"+ pattern.attributes().toString() );
			rc =   Plural.singular( prefix().toString()).contains( Plural.singular( pattern.prefix().toString()))
					&& postfix().equals( pattern.postfix())
					//&& content().matches( pattern.content())
					;
			//audit.debug("full check returns: "+ rc );
//		}
		//audit.traceut( rc );
		return rc;
	}

	// -- tag from string ctor
	private Patternette doPreamble() {
		int i = 0;
		String preamble = "";
		while (i < postfix().length() && '<' != postfix().charAt( i )) 
			preamble += postfix().charAt( i++ );
		prefix( new Strings( preamble ));
		if (i < postfix().length()) {
			i++; // read over terminator
			postfix( postfix().substring( i )); // ...save rest for later!
		} else
			postfix( "" );
		return this;
	}
	private Patternette doName() {
		int i = 0;
		while(i < postfix().length() && Character.isWhitespace( postfix().charAt( i ))) i++; //read over space following '<'
		if (i < postfix().length() && '/' == postfix().charAt( i )) {
			i++;
			type( END );
		}
		if (i < postfix().length()) {
			String name = "";
			while (i < postfix().length() && Character.isLetterOrDigit( postfix().charAt( i ))) name += postfix().charAt( i++ );
			name( name );
			
			if (i < postfix().length()) postfix( postfix().substring( i )); // ...save rest for later!
		} else
			name( "" ).postfix( "" );
		return this;
	}
	private Patternette doAttrs() {
		attributes( new Attributes( postfix() ));
		int i = attributes().nchars();
		//audit.log( "read "+ i +"chars in "+ attributes().toString() );
		//if (i>0) i++;
		if (i < postfix().length()) {
			//audit.log( "char at i="+ postfix().charAt( i ) );
			type( '/' == postfix().charAt( i ) ? ATOMIC : START );
			while (i < postfix().length() && '>' != postfix().charAt( i )) i++; // read to tag end
			if (i < postfix().length()) i++; // should be at '>' -- read over it
		}
		postfix( postfix().substring( i )); // save rest for later
		return this;
	}
	private Patternette doChildren() {
		if ( null != postfix() && !postfix().equals("") ) {
			Patternette child = new Patternette( postfix());
			while( NULL != child.type()) {
				// move child remained to tag...
				postfix( child.postfix());
				// ...add child, sans remainder, to tag content
				//content( child.postfix( "" ));
				child = new Patternette( postfix());
			}
			//postfix( child.postfix() ).content( child.postfix( "" ));
		}
		return this;
	}
	private Patternette doEnd() {
		name( "" ).type( NULL );
		int i = 0;
		while (i < postfix().length() && '>' != postfix().charAt( i )) i++; // read over tag end
		postfix( i == postfix().length() ? "" : postfix().substring( ++i ));
		return this;
	}
	// -- constructors...
	public Patternette() {}
	public Patternette( String cpp ) { // tagFromString()
		this();
		postfix( cpp );
		doPreamble().doName();
		if (type() == END) 
			doEnd();
		else {
			doAttrs();
			if (type() == START) doChildren();
	}	}
	// -- tag from string: DONE
	public Patternette( String pre, String nm ) {
		this();
		prefix( new Strings( pre )).name( nm );
	}
	public Patternette( String pre, String nm, String post ) {
		this( pre, nm );
		postfix( post );
	}
	public Patternette( Patternette orig ) {
		this( orig.prefix().toString(), orig.name(), orig.postfix());
		attributes( new Attributes( orig.attributes()));
		//content( orig.content());
	}

	private static Indent indent = new Indent( "  " );
	public String toXml() { return toXml( indent );}
	public String toXml( Indent indent ) {
		//int sz = content.size();
		//String contentSep = /*sz > 0? ("\n" + indent.toString()) : sz == 1 ? " " :*/ "";
		indent.incr();
		String    attrSep = /*sz > 0  ? "\n  "+indent.toString() :*/ " " ;
		String s = prefix().toString( Strings.OUTERSP )
				+ (name.equals( "" ) ? "" :
					("<"+ name
							+ attrs.toString( attrSep )
							+ //(0 == content().size() ? 
									"/>" //:
//									( ">"
//									+ content.toXml( indent )
//									+ contentSep
//									+ "</"+ name +">"
//				  )
									)		  )		//)
				+ postfix;
		indent.decr();
		return s;
	}
	public String toString() {
		return prefix().toString() + (name.equals( "" ) ? "" :
			("<"+ name +" "+ attrs.toString()+  // attributes doesn't have preceding space
			/*(0 == content().size() ?*/ "/>" /*: ( ">"+ content.toString() + "</"+ name +">" ))*/))
			+ postfix;
	}
	public String toText() {
		return prefix().toString()
			+ (prefix().toString()==null||prefix().toString().equals("") ? "":" ")
			+ (name.equals( "" ) ? "" :
				( name.toUpperCase( Locale.getDefault() ) +" "//+  // attributes has preceding space
					/*(0 == content().size() ? "" : content.toText() )*/))
			+ postfix;
	}
	public String toLine() {
		return prefix().toString()
				//+ (0 == content().size() ? "" : content.toText() )
				+ postfix ;
	}
	
	public static Patternette fromFile( String fname ) {
		Patternette t = null;
		try {
			t = new Patternette( Fs.stringFromStream( new FileInputStream( fname )) );
		} catch( IOException e ) {
			audit.ERROR( "no tag found in file "+ fname );
		}
		return t;
	}
	/*
	public static Tag fromAsset( String fname, Activity ctx ) {
		//audit.traceIn( "fromAsset", fname );
		Tag t = null;
		AssetManager am = ctx.getAssets();
		try {
			InputStream is = am.open( fname );
			t = new Tag( Filesystem.stringFromStream( is ));
			is.close();
		} catch (IOException e) {
			audit.ERROR( "no tag found in asset "+ fname );
		}
		//audit.traceut();
		return t;
	}
	*/
	public Patternette findByName( String nm ) {
		Patternette rc = null;
		if (name().equals( nm ))
			rc = this; // found
		else {
//			ArrayList<Patternette> ta = content();
//			for (int i=0; i<ta.size() && null == rc; ++i) // find first child
//				rc = ta.get( i ).findByName( nm );
		}
		return rc;
	}
	// -- test code
	public static void main( String argv[]) {
		Audit.allOn();
		audit.tracing = true;
		Strings a = new Strings( argv );
		int argc = argv.length;
		Patternette orig = new Patternette("prefix ", "util", "posstfix").append("sofa", "show").append("attr","one");
		//orig.content( new Patternette().prefix( new Strings( "show" )).name("sub"));
		//orig.content( new Patternette().prefix( new Strings( "fred") ));
		Patternette t = new Patternette( orig );
		//audit.log( "orig was:"+ orig.toString());
		//audit.audit( "copy: "+ t.toString());
		//t = new Tag( orig.toString());
		//audit.audit( "copy2: "+ t.toString());
		//Tag pattern = new Tag("prefix <util attr='one'/>posstfix");
		//audit.audit( "patt: "+ pattern.toString());
		//audit.audit( "orig "+ (orig.matchesContent( pattern )?"DOES":"does NOT") +" (and should) match pattern" );
		
		//t = new Tag("<test id='123' quantity='5 * 3 more'/>");
		//orig.update( t.attributes() );
		//audit.log( "orig is now:"+ orig.toString());
		
		t = new Patternette( "<xml>\n"+
			" <config \n"+
			"   CLASSPATH=\"/home/martin/ws/Enguage/bin\"\n"+
			"   DNU=\"I do not understand\" >\n" +	
            "   <concepts>\n"+
			"     <concept id=\"colloquia\"      op=\"load\"/>\n"+
			"     <concept id=\"needs\"          op=\"load\"/>\n"+
			"     <concept id=\"engine\"         op=\"load\"/>\n"+
			"   </concepts>\n"+
	        "   </config>\n"+
	        "   </xml>"        );
		audit.log( "tag:"+ t.toXml());
		
		if (argc > 0) {
			audit.log( "Comparing "+ t.toString() +", with ["+ a.toString( Strings.DQCSV ) +"]");
			//Attributes attr = t.content().matchValues( a );
			//audit.log( (null == attr ? "NOT " : "("+ attr.toString() +")" ) + "Matched" );
}	}	}
