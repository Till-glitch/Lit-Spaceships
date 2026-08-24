import net.minecraft.core.Direction;
public class Test {
    public static void main(String[] args) {
        for (Direction d : Direction.values()) {
            System.out.println(d.name() + ": " + d.getRotation());
        }
    }
}
