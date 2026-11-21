import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

class PatchWorldWeaverSave implements Opcodes {
    public static void main(String[] args) throws Exception {
        var target = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(target, 0);
        var methods = target.methods.stream().filter(method -> method.name.equals("saveFile")
                && method.desc.equals("(Lorg/betterx/wover/core/api/ModCore;)V")).toList();
        if (methods.size() != 1 || (methods.getFirst().access & (ACC_STATIC | ACC_SYNCHRONIZED)) != ACC_STATIC) {
            throw new IllegalStateException("Unexpected WorldWeaver config save method");
        }
        // All C2ME workers share the same native config temporary file.
        methods.getFirst().access |= ACC_SYNCHRONIZED;
        var writer = new ClassWriter(0);
        target.accept(writer);
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
