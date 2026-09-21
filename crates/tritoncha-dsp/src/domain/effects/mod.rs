//! DSP audio processors, filters, and spatial effects.

pub mod compressor;
pub mod delay;
pub mod fdn_reverb;
pub mod filter;
pub mod modular_chain;
pub mod processors;
pub mod reverb;

pub use compressor::{BusCompressor, CompressorConfig};
pub use delay::StereoDelay;
pub use fdn_reverb::FdnReverb;
pub use filter::{FilterMode, FrequencySweep, LadderFilter, StateVariableFilter};
pub use modular_chain::*;
pub use processors::*;
pub use reverb::{ReverbMode, StereoReverb};
