import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class PatchWorldWeaverEnd implements Opcodes {
    private static final String STREAM = "java/util/stream/Stream";
    private static final String CALLBACK = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    public static void main(String[] args) throws Exception {
        var target = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(target, 0);
        var method = target.methods.stream().filter(m -> m.name.equals("wover_collectPossibleBiomes"))
                .findFirst().orElseThrow();
        var inject = method.visibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")).findFirst()
                .orElseThrow();
        var points = (List<?>) inject.values.get(inject.values.indexOf("at") + 1);
        var at = (AnnotationNode) points.getFirst();
        int value = at.values.indexOf("value") + 1;
        if (points.size() != 1 || !at.values.get(value).equals("HEAD")) {
            throw new IllegalStateException("Unexpected WorldWeaver injection point");
        }
        at.values.set(value, "RETURN");

        // Keep the incoming biome stream so other RETURN hooks can compose with WorldWeaver.
        int patched = 0;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(CALLBACK)
                    && call.name.equals("setReturnValue")) {
                var combine = new InsnList();
                combine.add(new VarInsnNode(ALOAD, 1));
                combine.add(new MethodInsnNode(INVOKEVIRTUAL, CALLBACK, "getReturnValue",
                        "()Ljava/lang/Object;", false));
                combine.add(new TypeInsnNode(CHECKCAST, STREAM));
                combine.add(new InsnNode(SWAP));
                combine.add(new MethodInsnNode(INVOKESTATIC, STREAM, "concat",
                        "(Ljava/util/stream/Stream;Ljava/util/stream/Stream;)Ljava/util/stream/Stream;", true));
                combine.add(new MethodInsnNode(INVOKEINTERFACE, STREAM, "distinct",
                        "()Ljava/util/stream/Stream;", true));
                method.instructions.insertBefore(call, combine);
                patched++;
            }
        }
        if (patched != 1) throw new IllegalStateException("Unexpected WorldWeaver callback");
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
