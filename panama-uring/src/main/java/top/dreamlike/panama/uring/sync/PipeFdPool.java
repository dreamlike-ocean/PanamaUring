package top.dreamlike.panama.uring.sync;

import top.dreamlike.panama.uring.helper.pool.PooledObject;
import top.dreamlike.panama.uring.helper.pool.SimplePool;
import top.dreamlike.panama.uring.sync.fd.PipeFd;
import top.dreamlike.panama.uring.trait.OwnershipResource;

public class PipeFdPool extends SimplePool<PipeFdPool.OwnershipPipeFd> {

    public PipeFdPool(int max) {
        super(max);
    }

    @Override
    protected OwnershipPipeFd create() {
        return new OwnershipPipeFd(new PipeFd());
    }

    public class OwnershipPipeFd implements PooledObject<OwnershipPipeFd>, OwnershipResource<PipeFd> {

        private PipeFd pipeFd;

        public OwnershipPipeFd(PipeFd pipeFd) {
            this.pipeFd = pipeFd;
        }

        @Override
        public void destroy() {
            pipeFd.close();
        }

        @Override
        public PipeFd resource() {
            return pipeFd;
        }

        @Override
        public void drop() {
            OwnershipPipeFd fd = new OwnershipPipeFd(pipeFd);
            this.pipeFd = null;
            release(fd);
        }
    }
}
