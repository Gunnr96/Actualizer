import javax.swing.*;
import java.util.Map;
import java.util.concurrent.*;

public class ProgressWorker extends SwingWorker<Object, Object> {

    private Map<Integer, Standard> stds;

    public ProgressWorker(Map<Integer, Standard> stds) {
        this.stds = stds;
    }

    @Override
    protected Object doInBackground() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CompletionService<TwoTuple> compService = new ExecutorCompletionService<>(executor);


        for (Map.Entry<Integer, Standard> e : stds.entrySet()) {
            compService.submit(new Callable<TwoTuple>() {
                @Override
                public TwoTuple call() throws Exception {
                    return new TwoTuple(e.getKey(), StdChecker.check(e.getValue().number));
                }
            });
        }

        int progress = 0;
        int remainingFutures = stds.size();
        while (remainingFutures > 0) {
            Future<TwoTuple> future = compService.take();
            remainingFutures--;

            TwoTuple pair = future.get();
            stds.get(pair.index).status = pair.status;
            System.out.println(stds.get(pair.index).status);

            setProgress((int)Math.floor(((double) ++progress / stds.size()) * 100));
        }
        return null;
    }

    private class TwoTuple {
        private Integer index;
        private StdStatus status;

        private TwoTuple(Integer index, StdStatus status) {
            this.index = index;
            this.status = status;
        }
    }
}
