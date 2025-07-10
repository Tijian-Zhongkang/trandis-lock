package com.xm.sanvanfo.protocol;

import com.xm.sanvanfo.trandiscore.session.ReadWriteLockPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class ReentrantLockPath extends ReadWriteLockPath.LockPath {
    private static final long serialVersionUID = -8704597799207457096L;
    private Integer entrantTimes;

    public ReentrantLockPath(Integer entrantTimes,
            Long timeout, TimeUnit timeUnit, List<String> path) {
        super(path, timeout, timeUnit);
        this.entrantTimes = entrantTimes;
    }
}
