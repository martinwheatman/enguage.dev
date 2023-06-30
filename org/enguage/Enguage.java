package org.enguage;

import java.io.File;

import org.enguage.repertoires.Repertoires;
import org.enguage.repertoires.concepts.Autoload;
import org.enguage.repertoires.concepts.Concept;
import org.enguage.sign.Config;
import org.enguage.sign.object.list.Item;
import org.enguage.sign.object.sofa.Overlay;
import org.enguage.sign.symbol.Utterance;
import org.enguage.sign.symbol.reply.Reply;
import org.enguage.sign.symbol.where.Where;
import org.enguage.util.audit.Audit;
import org.enguage.util.strings.Strings;
import org.enguage.util.strings.Terminator;
import org.enguage.util.sys.Fs;
import org.enguage.util.sys.Shell;

public class Enguage {
	
	private static final String COPYRIGHT = "Martin Wheatman, 2001-4, 2011-23";
	public  static final String RO_SPACE  = "etc"+ File.separator;
	public  static final String RW_SPACE  = "var"+ File.separator;

	private static Enguage enguage;
	public  static Enguage get() {return enguage;}
	public  static void    set( Enguage e ) {enguage = e;}
	
	private static Audit   audit   = new Audit( "Enguage" );

	private static Shell   shell   = new Shell( "Enguage", COPYRIGHT );
	public  static Shell   shell() {return shell;}
	
	private static boolean verbose = false;
	public  static boolean isVerbose() {return verbose;}
	public  static void    verboseIs(boolean b) {verbose = b;}
	
	private boolean imagined = false;
	public  boolean imagined() {return imagined;}
	public  void    imagined( boolean img ) {imagined = img;}
		
	public  Enguage() {this( RW_SPACE );}
	public  Enguage( String root ) {
		Fs.root( root );
		Concept.addNames( Concept.list());
		Config.load( "config.xml" );
		Audit.resume();
	}
	
	private Strings mediateSingle( String uid, Strings utterance ) {
		audit.in("mediateSingle", "uid="+ uid +", utt="+ utterance );
		Strings reply;
	
		imagined( false );
		Overlay.attach( uid );
		Where.clearLocation();
		Item.resetFormat();
		Repertoires.signs().firstMatch( true );
		
		if (Reply.isUnderstood()) // from previous interpretation!
			Overlay.startTxn(); // all work in this new overlay
		
		Reply r = Repertoires.mediate( new Utterance( utterance ));

		// once processed, keep a copy
		Utterance.previous( utterance );

		if (imagined()) {
			Overlay.undoTxn();
			Repertoires.signs().reset( r.toStrings() );
			
		} else if (Reply.isUnderstood()) {
			Overlay.commitTxn();
			Repertoires.signs().reset( r.toStrings() );
			
		} else {
			// really lost track?
			audit.debug( "utterance is not understood, forgetting to ignore: "
			             +Repertoires.signs().ignore().toString() );
			Repertoires.signs().ignoreNone();
			shell.aloudIs( true ); // sets aloud for whole session if reading from fp
		}

		// auto-unload here - autoloading() in Repertoire.interpret() 
		// asymmetry: load as we go; tidy-up once finished
		Autoload.unloadAged();

		reply = Reply.say().appendAll( r.toStrings());
		Reply.say( null );
		Overlay.detach();
			
		audit.out();
		return reply;
	}
	public String mediate( String said ) {return mediate( Overlay.DEFAULT_USERID, said );}
	public String mediate( String uid, String said ) {
		audit.in( "mediate", "uid="+uid+", said="+said );
		Strings reply = new Strings();
		for (Strings conj : Concept.conjuntionAlley( new Strings( said ))) {
			if (!reply.isEmpty()) reply.add( "and" );
			Strings tmp = mediateSingle( uid, conj );
			reply.addAll( tmp );
		}
		audit.out( reply );
		return reply.toString();
	}
	
	/*
	 *  test code....
	 */
	public static void usage() {
		Audit.log( "Usage: java [-jar enguage.jar|org.enguage.Enguage]" );
		Audit.log( "            --help |" );
		Audit.log( "            --verbose --data <path>" ); 
		Audit.log( "            --port <port> [--httpd [--server <name>]] |" );
		Audit.log( "            --test | [<utterance>]" );
		Audit.log( "Options are:" );
		Audit.log( "       -h, --help" );
		Audit.log( "          displays this message\n" );
		Audit.log( "       -v, --verbose\n" );
		Audit.log( "       -d, --data <path> specifies the data volume to use\n" );
		Audit.log( "       -p, --port <port>" );
		Audit.log( "          defines a TCP/IP port number\n" );
		Audit.log( "       -H, --httpd" );
		Audit.log( "          use webserver protocols\n" );
		Audit.log( "       -s, --server <host> <port>" );
		Audit.log( "          switch to send speech to a server." );
		Audit.log( "          (Needs to be initialised with -p nnnn);\n" );
		Audit.log( "       [<utterance>]" );
		Audit.log( "          with an utterance it runs one-shot;" );
		Audit.log( "          with no utterance it runs as a shell," );
		Audit.log( "             requiring full stops (periods) to" );
		Audit.log( "             terminate utterances." );
	}
	
	public static void main( String[] args ) {
		
		Strings    cmds = new Strings( args );
		String     cmd;
		String     fsys = RW_SPACE;
		
		// traverse args and strip switches: -v -d -h
		int i = 0;
		while (i < cmds.size()) {
			
			cmd = cmds.get( i );
			
			if        (cmd.equals( "-h" ) || cmd.equals( "--help" )) {
				Enguage.usage();
				System.exit( 0 );
			
			} else if (cmd.equals( "-v" ) || cmd.equals( "--verbose" )) {
				cmds.remove( i );
				verbose = true;
					
			} else if (cmd.equals( "-d" ) || cmd.equals( "--data" )) {
				cmds.remove( i );
				fsys = cmds.isEmpty() ? fsys : cmds.remove( i );
				
			} else
				i++;
		}

		enguage = new Enguage( fsys );
				
		cmd = cmds.isEmpty() ? "":cmds.remove( 0 );
		
		if (cmd.equals( "" )) {
			Overlay.attach( "uid" );
			shell.aloudIs( true ).run();
		
		} else {
			// Command line parameters exists...
			// reconstruct original commands
			cmds.prepend( cmd );
			audit.in( "CLI:", ""+cmds );

			Terminator.stripTerminator( cmds );
			
			// ...and interpret
			audit.out( Enguage.get().mediate( cmds.toString() ));
}	}	}
