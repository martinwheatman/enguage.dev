package org.enguage.signs.objects.space;

import java.io.File;
import java.util.Iterator;

import org.enguage.util.Audit;
import org.enguage.util.Strings;
import org.enguage.util.sys.Fs;
import org.enguage.util.sys.Path;
import org.enguage.util.sys.Pent;
import org.enguage.util.sys.Shell;

class OverlayShell extends Shell {
	OverlayShell( Strings args ) { super( Overlay.NAME, args ); }
	public Strings interpret( Strings argv ) { return interpret( argv ); }
}

public class Overlay {
	public static  final String NAME = "overlay";
	static               Audit audit = new Audit( "Overlay" );
	public static  final int      id = 188374473; //Strings.hash( "overlay" );
	
	private static final char   DELETE_CH  = '!';  // RENAME_CH = "^"
	private static final String DELETE_STR = ""+DELETE_CH;
	private static final String   DETACHED = null;
	
	public static  final String    DEFAULT = "Enguage"; //"sofa"
	public static  final int   MODE_READ   = 0; // "r"
	public static  final int   MODE_WRITE  = 1; // "w"
	public static  final int   MODE_APPEND = 2; // "a"
	public static  final int   MODE_DELETE = 3; // "d"
	//public static final int  MODE_RENAME = 4; // "m" 
	
	// manage singleton
	private static Overlay overlay = new Overlay();
	public  static Overlay get() {return overlay;}
	public  static void    set( Overlay o ) {overlay = o;}
	
	private Path p;
	String  path() {return p.toString();}
	public boolean path( String dest ) {
		boolean rc = true;
		if (null != dest) {
			String src = p.pwd();    // remember where we are
			p.cd( dest );            // do the cd
			// check destination - might only exist in object space
			rc = Fs.existsEntity( fname( p.pwd(), MODE_READ ));
			if (!rc) p = new Path( src ); // return to where we were
		}
		return rc;
	}
 
	public Overlay() {
		p = new Path( System.getProperty( "user.dir" ));
	}
	
	// --- object space - just write directly into Fs.root()
	private static String root = "";
	public  static void   root( String uid ) {
		root = Fs.root()+ uid +File.separator;
		new File( root ).mkdirs();
	}
	
	// --- Series management
	private static String series = DETACHED;
	private static void   series( String nm ) {if (nm != null) series = nm; }
	
	private static int     highest = -1; // 0, 1, ..., n => 1+n; -1 == detached
	private static void    countOverlays() {
		highest = -1;
		String[] files = new File( root ).list();
		if (files != null) for (String file : files)
			try {	
				Integer.parseInt( file );
				highest++;
			} catch (Exception ex) {/*ignore*/}
	}
	private static int     highestOverlay() {return highest;}
	
	private static boolean attached = false;
	private static boolean attached() {return attached;}
	private static void    attached(boolean b) {attached = b;}
	
	private static String  nth( int vn ) {return root + vn;}
	
	public  static void attach( String userId ) {
		if (!attached()) {
			root( userId );
			set( get()); // set singleton
			String cwd = System.getProperty( "user.dir" );
			series( new File( cwd ).getName() );
			Link.fromString( root + series, cwd );
			countOverlays();
			attached( true );
	}	}
	public static  void    detach() {
		if (attached()) {
			series( DETACHED );
			attached( false );
	}	}

	public static  boolean exists() {return Fs.exists( root+ series + Link.EXT );}
	public static  void    append() {if (attached()) new File( nth( highest++ )).mkdirs();}
	public static  boolean remove() {
		return highestOverlay()+1 >= 0 && attached() &&
				Fs.destroy( nth( --highest ));
	}

	private String nthCandidate( String nm, int vn ) {
		int topVn;
		return nth( vn < 0                          ? 0 :
		            vn > (topVn = highestOverlay()) ? topVn : vn
		          ) + File.separator + nm;
	}
	private String topCandidate( String name ) {
		return nthCandidate( name, highestOverlay());
	}
	private String delCandidate( String name, int n ) {
		File f = new File( nthCandidate( name, n ));
		return f.getParent() +"/"+ DELETE_STR + f.getName(); 
	}
	private String find( String vfname ) {
		String fsname = null;
		int vn = highestOverlay()+1; // number=3 ==> 0,1,2, so initially decr vn
		while (--vn >= 0)
			if (Fs.exists( fsname = nthCandidate( vfname, vn ) ))
				return fsname;
			else if (Fs.exists( delCandidate( vfname, vn ) )) 
				return topCandidate( vfname ); // look no further - return top (non-existing) file	
		return vfname;
	}
	private boolean isOverlaid( String vfname ) {
		if (vfname == null) return false;
		if (vfname.startsWith( root )) return false;
		if (!vfname.startsWith( "/" )) return true; // assumes were at head of overlaid fs!
		return vfname.startsWith( Link.content( root + series )); // NAME="xyz/abc" base="xyz" => true
	}
	
	//  virtual to "real" filename mapping
	// /home/martin/person/martin/name.txt -> /home/martin/sofa/sofa.0/martin/name.txt
	// if write - top overlay NAME is returned, simple.
	// if read  - overlay space is searched for an existing file.
	//          - if not found, or if the file has been deleted, the (non-existing) write filename is returned
	//          - if rename found (e.g. old^new), change return the old NAME.
	public static String fname( String vfname, int modeChs ) {
		String fsname = vfname; // pass through!
		if (attached() && overlay.isOverlaid( vfname ))
			switch (modeChs) {
				case MODE_READ   : fsname = overlay.find( vfname ); break;
				case MODE_DELETE : fsname = overlay.delCandidate( vfname, highestOverlay() ); break;
				default          : fsname = overlay.topCandidate( vfname ); // MODE_WRITE | _APPEND
			}
		return fsname;
	}
	
	public static boolean isDeleteName( String name ) {
		return new File( name ).getName().charAt( 0 ) == DELETE_CH;
	}
	public static String nonDeleteName( String name ) {
		if (isDeleteName( name )) {
			File f = new File( name );
			name = f.getParent() +"/"+ f.getName().substring( 1 );
		}
		return name;
	}
	public static String deleteName( String name ) {
		if (!isDeleteName( name )) {
			File f = new File( name );
			name = f.getParent() +"/"+ DELETE_STR + f.getName();
		}
		return name;
	}
	
	public Strings list( String dname ) {
		
		/* dname will usually be ".", or "1subDir"
		 * 
		 * vpwd is: /home/martin/src/files/
		 * maps to: /var/overlays/enguage.0/files
		 * and    : /var/overlays/enguage.0/files/1subDir
		 * NB. 1subDir doesn't "really" exist
		 * 
		 * this needs to read:
		 * 
		 * /home/martin/yagadi/enguage.0/pwd/dna/me/
		 * /home/martin/yagadi/enguage.1/pwd/dna/me/
		 * ...
		 * /home/martin/yagadi/enguage.n/pwd/dna/me/
		 * 
		 * adding and removing files to build up a state-of-affairs.
		 */
		Strings rc = new Strings();
		p.pathListDelete();
		
		String absName = null;
		try {
			if (null == dname) {
				absName =  ".";
			} else if (dname.charAt( 0 ) == '/') {
				absName = new File( dname ).getCanonicalPath();
			} else {
				absName = new File( p.pwd() + File.separator + dname ).getCanonicalPath();
			}
		} catch( Exception e ) {
			audit.error( "cannonical file error:"+ e.toString() );
		}
		
		p.insertDir( absName, "" );
		if (p.pwd().length() <= absName.length() && isOverlaid( p.pwd() )) {
			int     n = -1;
			int count = highestOverlay()+1;
			String suffix = absName.substring( p.pwd().length());
			while (++n < count)
				p.insertDir( root + n + suffix, "" );
		}
		Iterator<Pent> pi = p.iterator(); 
		while ( pi.hasNext() )
			rc.add( pi.next().name() );
		
		return rc;
	}
	
	// ===
	
	/*
	 * implements transactions -- but isn't ACID :(
	 */
	// --- Compact overlays
	private static void moveFiles( File src, File dest ) {
		if (src.isDirectory()) {
			if (!dest.exists()) dest.mkdir();
			String[] files = src.list();
			if (files != null)
				for (String file : files)
					moveFiles( new File( src, file ), new File( dest, file ));
			src.delete();
			
		} else {  // move file across
			if (isDeleteName( src.toString())) // We have !filename...
				// remove filename
				new File( nonDeleteName( dest.toString() )).delete();
			
			src.renameTo( dest );
	}	}
	
	private static boolean compact() {
		if (attached()) {
			
			int overlay = highestOverlay()-1;
			while (overlay>0) {
				moveFiles( 
					new File( nth( overlay )),
					new File( nth( overlay-1 ))
				);
				overlay--;
			}
			
			// then _rename_ top overlay - will now be ".1"
			File src = new File( nth( highestOverlay() ));
			File dst = new File( nth( 1 ));
			src.renameTo( dst );
			
			// reset overlay count
			countOverlays();
			return true;
		}
		return false;
	}

	// --- Transactions
	private static boolean inprog = false;
	public  static void finishTxn() {
		if (inprog) {
			compact();
			inprog = false;
	}	}
	public  static void abortTxn()  {
		if (inprog) {
			remove();
			inprog = false;
	}	}
	public  static void startTxn()  {
		if (!inprog) {
			append();
			inprog = true;
	}	}
	public  static void reStartTxn() {
		remove(); // remove this overlay
		remove(); // remove previous -- this is the undo bit
		append(); // restart a new txn
	}

	// --- Test code...
	
	public static Strings interpret( Strings argv ) {
		String rc = Shell.FAIL;
		int argc = argv.size();
		
		String  cmd    = argv.remove( 0 );
		String  value  = argv.toString( Strings.PATH );
		
		if (cmd.equals("attach") && (2 >= argc)) {
			if (2 == argc) {
				countOverlays();
				if (highest < 0)
					audit.debug( "No such series: "+ value );
				else
					rc = Shell.SUCCESS;
			} else if ( attached())
				audit.debug( "Not attached" );
			else
				rc = Shell.SUCCESS;
				
		} else if (cmd.equals("detach") && (1 == argc)) {
			detach();
			rc = Shell.SUCCESS;
			
		} else if ((cmd.equals( "save" ) || cmd.equals( "create" )) && (1 == argc)) {
			rc = Shell.SUCCESS;
			append();
			
		} else if (cmd.equals( "exists" ) && (2 == argc)) {
			rc =  exists() ? "Yes":"No";
						
		} else if (cmd.equals(  "destroy"  ) && (1 == argc) ) {
			rc = remove() ? Shell.SUCCESS : Shell.FAIL;
			
		} else if ((   cmd.equals(    "bond"  )
				    || cmd.equals( "commit"  ))
		           && (1 == argc) ) {
			rc = compact() ? Shell.SUCCESS : Shell.FAIL;
			
		} else if (cmd.equals( "rm" )) {
			argv.remove( 0 );
			String fname = argv.remove( argv.size()-1 );
			rc = new Value( argv.toString( Strings.CONCAT ), fname ).ignore() ? Shell.SUCCESS : Shell.FAIL;
		
		} else if (cmd.equals( "write" )) {
			rc = Shell.SUCCESS;
			Audit.log( "New file would be '"+ fname( value, MODE_WRITE ) +"'" ); // last param ignored
			
		} else if (cmd.equals( "mkdir" ) && (2 == argc)) {
			rc = Shell.SUCCESS;
			String fname = fname( argv.get( 1 ), MODE_WRITE );
			Audit.log( "Fname is "+ fname );
			Audit.log( ">>>mkdir("+ fname +") => "+ (new File( fname ).mkdirs()?"Ok":"Error"));
			
		} else if (cmd.equals( "read" ) && (2 == argc)) {
			rc = Shell.SUCCESS;
			Audit.log( "File found? is '"+ fname( value, MODE_READ )+"'" );
			
		} else
			audit.debug( "Usage: attach <series>\n"
			                 +"     : detach\n"
			                 +"     : save\n"
			                 +"     : write <pathname>\n"
			                 +"     : read  <pathname>\n"
			                 +"     : mkdir <pathname>\n"
			                 +"     : pwd <pathname>\n"
			                 +"     : cd <pathname>\n"
			                 +"     : ls <pathname>\n"
			                 +"given: "+ argv.toString( Strings.CSV ));
		return new Strings( rc );
	}
	
	public static void main (String args []) {
		Audit.on();
		attach( "Overlay" );
		Audit.log( "osroot="+ root );
		Audit.log( "base="+ series+", n=" + (highestOverlay()+1) );
		OverlayShell os = new OverlayShell( new Strings( args ));
		os.run();
}	}
