package com.github.goldin.plugins.silencer

import static com.github.goldin.plugins.common.GMojoUtils.*
import com.github.goldin.plugins.common.BaseGroovyMojo
import org.apache.maven.lifecycle.internal.MojoExecutor
import org.apache.maven.plugin.DefaultBuildPluginManager
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.plexus.PlexusConstants
import org.codehaus.plexus.PlexusContainer
import org.codehaus.plexus.context.Context
import org.codehaus.plexus.context.ContextException
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable
import org.gcontracts.annotations.Requires
import java.lang.reflect.Field
import java.lang.reflect.Modifier


/**
 * Mojo for silencing Maven logging.
 */
@Mojo( name = 'silence', defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
class SilencerMojo extends BaseGroovyMojo implements Contextualizable
{

    static final SilentLogger SILENT_LOGGER = new SilentLogger()

    @Parameter ( required = false )
    private String enabled


    private PlexusContainer container

    @Override
    @Requires({ context })
    void contextualize ( Context context ) throws ContextException
    {
        this.container = ( PlexusContainer ) context.get( PlexusConstants.PLEXUS_KEY )
    }


    @Override
    void doExecute()
    {
        if (( enabled == null ) || ( groovyBean().eval( enabled, Boolean )))
        {
            log.info( 'Silencer Mojo is on - enjoy the silence.' )
            updateMavenPluginManager()
            updateRepositoryLogger()
        }
    }


    private void updateMavenPluginManager ()
    {
        try
        {
            final executor = container.lookup( MojoExecutor )
            (( DefaultBuildPluginManager ) executor.pluginManager ).mavenPluginManager =
                new SilencerMavenPluginManager((( DefaultBuildPluginManager ) executor.pluginManager ).mavenPluginManager )
        }
        catch ( Throwable e ){ e.printStackTrace()}
    }


    private void updateRepositoryLogger ()
    {
        try
        {
            final listener            = session.repositorySession.repositoryListener
            final loggerField         = listener.class.getDeclaredField( 'logger' )
            final modifiersField      = Field.class.getDeclaredField( 'modifiers' )
            loggerField.accessible    = true
            modifiersField.accessible = true

            modifiersField.setInt( loggerField, loggerField.modifiers & ~Modifier.FINAL )
            loggerField.set( listener, SILENT_LOGGER )
        }
        catch ( Throwable e ){ e.printStackTrace()}
    }
}