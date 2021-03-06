//package nars.task.util;
//
//import nars.Task;
//import nars.task.Tasked;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Objects;
//
//public class TaskRegionLink extends TasksRegion {
//
//    @NotNull
//    public final Task task;
//
//    protected TaskRegionLink(Task task, float freq, float conf) {
//        super(task.start(), task.end(), freq, freq, conf, conf);
//        this.task = task;
//    }
//
//    public static TaskRegionLink link(Task task) {
//        return new TaskRegionLink(task, task.freq(), task.conf());
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        return this == obj || (obj != null && Objects.equals(task, ((Tasked) obj).task()));
//    }
//
//    @Override
//    public int hashCode() {
//        return task.hashCode();
//    }
//
//    @Override
//    public String toString() {
//        return task.toString();
//    }
//
//    @Override
//    public @Nullable final Task task() {
//        return task;
//    }
//}
