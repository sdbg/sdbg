package subprojects;
public class SdbgDebugCoreBuild extends AbstractSdbgBuild
{
    public SdbgDebugCoreBuild()
    {
        super("com.github.sdbg.debug.core");
    }
    
    @Override
    public void compile() throws Exception
    {
//        addSiblingJar("com.gwtplugins.gdt.eclipse.core");

        super.compile();
    }

}
