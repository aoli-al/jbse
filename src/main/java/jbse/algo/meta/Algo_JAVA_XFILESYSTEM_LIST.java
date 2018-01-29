package jbse.algo.meta;

import static jbse.algo.Util.exitFromAlgorithm;
import static jbse.algo.Util.failExecution;
import static jbse.algo.Util.throwNew;
import static jbse.algo.Util.throwVerifyError;
import static jbse.algo.Util.valueString;
import static jbse.bc.Signatures.JAVA_FILE_PATH;
import static jbse.bc.Signatures.JAVA_STRING;
import static jbse.bc.Signatures.OUT_OF_MEMORY_ERROR;
import static jbse.common.Type.ARRAYOF;
import static jbse.common.Type.TYPEEND;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.StrategyUpdate;
import jbse.algo.exc.SymbolicValueNotAllowedException;
import jbse.bc.ClassFile;
import jbse.bc.exc.ClassFileIllFormedException;
import jbse.bc.exc.ClassFileNotAccessibleException;
import jbse.bc.exc.ClassFileNotFoundException;
import jbse.common.exc.ClasspathException;
import jbse.mem.Array;
import jbse.mem.Instance;
import jbse.mem.State;
import jbse.mem.exc.FastArrayAccessNotAllowedException;
import jbse.mem.exc.HeapMemoryExhaustedException;
import jbse.tree.DecisionAlternative_NONE;
import jbse.val.Calculator;
import jbse.val.Null;
import jbse.val.Reference;
import jbse.val.ReferenceConcrete;

/**
 * Meta-level implementation of {@link java.io.UnixFileSystem#list(File)} and
 * {@link java.io.WinNTFileSystem#list(File)}
 * 
 * @author Pietro Braione
 */
public final class Algo_JAVA_XFILESYSTEM_LIST extends Algo_INVOKEMETA_Nonbranching {
    private String[] theList; //set by cookMore

    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 2;
    }

    @Override
    protected void cookMore(State state) 
    throws InterruptException, ClasspathException, SymbolicValueNotAllowedException {
        try {
            //gets the File parameter: if null, the attributes are 0
            final Reference fileReference = (Reference) this.data.operand(1);
            if (state.isNull(fileReference)) {
                this.theList = null;
                return;
            }
            final Instance fileObject = (Instance) state.getObject(fileReference);
            if (fileObject == null) {
                //this should never happen
                failExecution("The File f parameter to invocation of method java.io.UnixFileSystem.getBooleanAttributes0 was an unresolved symbolic reference.");
            }
            
            //gets the path field as a String
            final Reference filePathReference = (Reference) fileObject.getFieldValue(JAVA_FILE_PATH);
            if (filePathReference == null) {
                throwVerifyError(state);
                exitFromAlgorithm();
            }
            final String filePath = valueString(state, filePathReference);
            if (filePath == null) {
                throw new SymbolicValueNotAllowedException("The File f parameter to invocation of method java.io.UnixFileSystem.getBooleanAttributes0 has a symbolic String in its path field.");
            }
            
            //creates a File object with same path and
            //invokes metacircularly the java.io.UnixFileSystem.getBooleanAttributes0
            //method to obtain its attributes
            final Field fileSystemField = File.class.getDeclaredField("fs");
            fileSystemField.setAccessible(true);
            final Object fileSystem = fileSystemField.get(null);
            final Class<?> fileSystemClass = fileSystem.getClass(); 
            final Method listMethod = fileSystemClass.getDeclaredMethod("list", File.class);
            listMethod.setAccessible(true);
            final File f = new File(filePath);
            this.theList = (String[]) listMethod.invoke(fileSystem, f);
        } catch (ClassCastException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            //this should not happen
            failExecution(e);
        }
    }

    @Override
    protected StrategyUpdate<DecisionAlternative_NONE> updater() {
        return (state, alt) -> {
            if (this.theList == null) {
                state.pushOperand(Null.getInstance());
                return;
            }
            
            try {
                //creates an Array and fills it with the result
                final Calculator calc = state.getCalculator();
                final ClassFile cf_arrayOfJAVA_STRING = state.getClassHierarchy().loadCreateClass("" + ARRAYOF + JAVA_STRING + TYPEEND);
                final ReferenceConcrete arrayRef = state.createArray(null, calc.valInt(this.theList.length), cf_arrayOfJAVA_STRING);
                final Array array = (Array) state.getObject(arrayRef);
                for (int i = 0; i < this.theList.length; ++i) {
                    state.ensureStringLiteral(this.theList[i]);
                    final ReferenceConcrete stringRef = state.referenceToStringLiteral(this.theList[i]);
                    array.setFast(calc.valInt(i), stringRef);
                }
                
                //pushes the reference to the array
                state.pushOperand(arrayRef);
            } catch (HeapMemoryExhaustedException e) {
                throwNew(state, OUT_OF_MEMORY_ERROR);
                exitFromAlgorithm();
            } catch (ClassFileNotFoundException | ClassFileIllFormedException | 
                     ClassFileNotAccessibleException | FastArrayAccessNotAllowedException e) {
                //this should never happen
                failExecution(e);
            }
        };
    }
}