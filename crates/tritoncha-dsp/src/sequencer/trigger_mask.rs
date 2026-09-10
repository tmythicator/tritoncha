//! Typed sequencer trigger mask representation.

/// Strongly-typed 32-bit track trigger bitmask.
#[derive(Default, Copy, Clone, Debug, PartialEq, Eq)]
pub struct TriggerMask(pub u32);

impl TriggerMask {
    /// Creates an empty trigger mask.
    #[inline(always)]
    pub const fn new() -> Self {
        Self(0)
    }

    /// Marks a specific track slot index as triggered.
    #[inline(always)]
    pub fn set_track(&mut self, slot: usize) {
        if slot < 32 {
            self.0 |= 1 << slot;
        }
    }

    /// Tests if a specific track slot index was triggered.
    #[inline(always)]
    pub fn is_triggered(&self, slot: usize) -> bool {
        if slot < 32 {
            (self.0 & (1 << slot)) != 0
        } else {
            false
        }
    }

    /// Returns the raw 32-bit unsigned integer value.
    #[inline(always)]
    pub const fn as_u32(&self) -> u32 {
        self.0
    }

    /// Clears all trigger bits back to 0.
    #[inline(always)]
    pub fn clear(&mut self) {
        self.0 = 0;
    }

    /// Extracts and resets the accumulated mask in one operation.
    #[inline(always)]
    pub fn take(&mut self) -> u32 {
        let mask = self.0;
        self.0 = 0;
        mask
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_trigger_mask_set_and_query() {
        let mut mask = TriggerMask::new();
        assert_eq!(mask.as_u32(), 0);
        assert!(!mask.is_triggered(0));
        assert!(!mask.is_triggered(3));

        mask.set_track(0);
        mask.set_track(3);
        assert!(mask.is_triggered(0));
        assert!(mask.is_triggered(3));
        assert!(!mask.is_triggered(1));
        assert_eq!(mask.as_u32(), 0b1001);

        let extracted = mask.take();
        assert_eq!(extracted, 0b1001);
        assert_eq!(mask.as_u32(), 0);
    }
}
