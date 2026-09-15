//! Modular synthesizer sound generation, patches, oscillators, and envelopes.

pub mod envelope;
pub mod oscillator;
pub mod patch;
pub mod presets;
pub mod voice;

pub use envelope::*;
pub use oscillator::*;
pub use patch::*;
pub use voice::*;
